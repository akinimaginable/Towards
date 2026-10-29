#define WIN32_LEAN_AND_MEAN
#define NOMINMAX

#include <windows.h>

#include <winrt/Windows.Devices.Geolocation.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Security.Authorization.AppCapabilityAccess.h>

#include <chrono>
#include <functional>
#include <mutex>
#include <thread>

#include "location.h"

using winrt::Windows::Devices::Geolocation::GeolocationAccessStatus;
using winrt::Windows::Devices::Geolocation::Geolocator;
using winrt::Windows::Devices::Geolocation::Geoposition;
using winrt::Windows::Devices::Geolocation::PositionStatus;
using winrt::Windows::Foundation::AsyncStatus;
using winrt::Windows::Foundation::IAsyncOperation;
using winrt::Windows::Foundation::TimeSpan;
using winrt::Windows::Security::Authorization::AppCapabilityAccess::AppCapability;
using winrt::Windows::Security::Authorization::AppCapabilityAccess::AppCapabilityAccessStatus;

namespace {

constexpr int kStatusNotDetermined = 0;
constexpr int kStatusDenied = 1;
constexpr int kStatusGranted = 2;
constexpr int kStatusUnavailable = 3;
constexpr int kCodeSuccess = 0;
constexpr int kCodeDenied = 1;
constexpr int kCodeUnavailable = 2;
constexpr UINT kSyncMessage = WM_APP;
constexpr UINT kAsyncMessage = WM_APP + 1;

struct SyncRequest {
    std::function<void()> fn;
    HANDLE done;
};

HWND staWindow = nullptr;
DWORD staThreadId = 0;
HANDLE staReady = nullptr;
bool staFailed = false;
std::once_flag staOnce;

Geolocator geolocator{nullptr};
IAsyncOperation<Geoposition> positionOp{nullptr};
towards_location_callback currentCallback = nullptr;
int currentGeneration = 0;
int grantedThisProcess = 0;
double cachedLatitude = 0;
double cachedLongitude = 0;
bool hasCachedFix = false;

LRESULT CALLBACK StaWndProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
        case kSyncMessage: {
            auto *request = reinterpret_cast<SyncRequest *>(lParam);
            try {
                request->fn();
            } catch (...) {
            }
            SetEvent(request->done);
            return 0;
        }
        case kAsyncMessage: {
            auto *fn = reinterpret_cast<std::function<void()> *>(lParam);
            try {
                (*fn)();
            } catch (...) {
            }
            delete fn;
            return 0;
        }
        default:
            return DefWindowProcW(window, message, wParam, lParam);
    }
}

void staThreadMain() {
    try {
        winrt::init_apartment(winrt::apartment_type::single_threaded);
        WNDCLASSW windowClass{};
        windowClass.lpfnWndProc = StaWndProc;
        windowClass.hInstance = GetModuleHandleW(nullptr);
        windowClass.lpszClassName = L"TowardsLocationSta";
        RegisterClassW(&windowClass);
        // RequestAccessAsync only succeeds on a thread that owns a window and pumps messages.
        staWindow = CreateWindowExW(
            0,
            windowClass.lpszClassName,
            L"Towards",
            WS_OVERLAPPED,
            0,
            0,
            0,
            0,
            nullptr,
            nullptr,
            windowClass.hInstance,
            nullptr);
        staThreadId = GetCurrentThreadId();
        staFailed = staWindow == nullptr;
    } catch (...) {
        staFailed = true;
    }
    SetEvent(staReady);
    if (staFailed) {
        return;
    }
    MSG message;
    while (GetMessageW(&message, nullptr, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }
}

void ensureSta() {
    std::call_once(staOnce, [] {
        staReady = CreateEventW(nullptr, TRUE, FALSE, nullptr);
        std::thread(staThreadMain).detach();
    });
    WaitForSingleObject(staReady, 10000);
    if (staFailed || staWindow == nullptr) {
        throw winrt::hresult_error(E_FAIL);
    }
}

void invokeSync(std::function<void()> fn) {
    ensureSta();
    if (GetCurrentThreadId() == staThreadId) {
        fn();
        return;
    }
    SyncRequest request{std::move(fn), CreateEventW(nullptr, FALSE, FALSE, nullptr)};
    if (!PostMessageW(staWindow, kSyncMessage, 0, reinterpret_cast<LPARAM>(&request))) {
        CloseHandle(request.done);
        throw winrt::hresult_error(E_FAIL);
    }
    WaitForSingleObject(request.done, INFINITE);
    CloseHandle(request.done);
}

void invokeAsync(std::function<void()> fn) {
    ensureSta();
    if (GetCurrentThreadId() == staThreadId) {
        fn();
        return;
    }
    auto *heap = new std::function<void()>(std::move(fn));
    if (!PostMessageW(staWindow, kAsyncMessage, 0, reinterpret_cast<LPARAM>(heap))) {
        delete heap;
        throw winrt::hresult_error(E_FAIL);
    }
}

void ensureGeolocator() {
    if (geolocator) {
        return;
    }
    geolocator = Geolocator();
    geolocator.DesiredAccuracyInMeters(winrt::Windows::Foundation::IReference<uint32_t>(100));
}

bool servicesAvailable() {
    ensureGeolocator();
    auto status = geolocator.LocationStatus();
    return status != PositionStatus::Disabled && status != PositionStatus::NotAvailable;
}

int checkAccess() {
    ensureGeolocator();
    auto capability = AppCapability::Create(L"location");
    switch (capability.CheckAccess()) {
        case AppCapabilityAccessStatus::Allowed:
            grantedThisProcess = 1;
            return servicesAvailable() ? kStatusGranted : kStatusUnavailable;
        case AppCapabilityAccessStatus::UserPromptRequired:
        case AppCapabilityAccessStatus::NotDeclaredByApp:
            if (grantedThisProcess) {
                return servicesAvailable() ? kStatusGranted : kStatusUnavailable;
            }
            return kStatusNotDetermined;
        case AppCapabilityAccessStatus::DeniedByUser:
        case AppCapabilityAccessStatus::DeniedBySystem:
            grantedThisProcess = 0;
            return kStatusDenied;
        default:
            return kStatusUnavailable;
    }
}

void postCallback(towards_location_callback callback, int code, double latitude, double longitude) {
    if (callback == nullptr) {
        return;
    }
    // Leave the STA before entering the JVM so a re-entrant status read cannot deadlock it.
    std::thread([callback, code, latitude, longitude] {
        callback(code, latitude, longitude);
    }).detach();
}

void deliverCurrent(int code, double latitude, double longitude) {
    towards_location_callback callback = currentCallback;
    currentCallback = nullptr;
    postCallback(callback, code, latitude, longitude);
}

}  // namespace

int location_authorization_status(void) {
    int status = kStatusUnavailable;
    try {
        invokeSync([&] { status = checkAccess(); });
    } catch (...) {
        status = kStatusUnavailable;
    }
    return status;
}

int location_last_known(double *latitude, double *longitude) {
    if (latitude == nullptr || longitude == nullptr) {
        return 0;
    }
    int found = 0;
    try {
        invokeSync([&] {
            if (!hasCachedFix) {
                return;
            }
            *latitude = cachedLatitude;
            *longitude = cachedLongitude;
            found = 1;
        });
    } catch (...) {
        return 0;
    }
    return found;
}

void location_request_permission(towards_location_callback callback) {
    try {
        invokeAsync([callback] {
            try {
                auto operation = Geolocator::RequestAccessAsync();
                operation.Completed([callback](auto const &asyncOp, AsyncStatus status) {
                    int code = kCodeUnavailable;
                    if (status == AsyncStatus::Completed) {
                        try {
                            switch (asyncOp.GetResults()) {
                                case GeolocationAccessStatus::Allowed:
                                    grantedThisProcess = 1;
                                    code = kCodeSuccess;
                                    break;
                                case GeolocationAccessStatus::Denied:
                                    grantedThisProcess = 0;
                                    code = kCodeDenied;
                                    break;
                                default:
                                    code = kCodeUnavailable;
                                    break;
                            }
                        } catch (...) {
                            code = kCodeUnavailable;
                        }
                    } else if (status == AsyncStatus::Canceled) {
                        return;
                    }
                    postCallback(callback, code, 0, 0);
                });
            } catch (...) {
                postCallback(callback, kCodeUnavailable, 0, 0);
            }
        });
    } catch (...) {
        postCallback(callback, kCodeUnavailable, 0, 0);
    }
}

void location_request_current(towards_location_callback callback) {
    try {
        invokeAsync([callback] {
            try {
                deliverCurrent(kCodeUnavailable, 0, 0);
                if (positionOp) {
                    positionOp.Cancel();
                    positionOp = nullptr;
                }
                currentGeneration += 1;
                int generation = currentGeneration;
                ensureGeolocator();
                if (!servicesAvailable()) {
                    postCallback(callback, kCodeUnavailable, 0, 0);
                    return;
                }
                if (checkAccess() != kStatusGranted) {
                    postCallback(callback, kCodeDenied, 0, 0);
                    return;
                }
                currentCallback = callback;
                auto timeout = std::chrono::duration_cast<TimeSpan>(std::chrono::seconds(15));
                positionOp = geolocator.GetGeopositionAsync(TimeSpan{0}, timeout);
                positionOp.Completed([generation](auto const &asyncOp, AsyncStatus status) {
                    if (generation != currentGeneration) {
                        return;
                    }
                    if (status == AsyncStatus::Canceled || currentCallback == nullptr) {
                        positionOp = nullptr;
                        return;
                    }
                    if (status != AsyncStatus::Completed) {
                        positionOp = nullptr;
                        deliverCurrent(kCodeUnavailable, 0, 0);
                        return;
                    }
                    try {
                        auto position = asyncOp.GetResults().Coordinate().Point().Position();
                        cachedLatitude = position.Latitude;
                        cachedLongitude = position.Longitude;
                        hasCachedFix = true;
                        positionOp = nullptr;
                        deliverCurrent(kCodeSuccess, cachedLatitude, cachedLongitude);
                    } catch (...) {
                        positionOp = nullptr;
                        deliverCurrent(kCodeUnavailable, 0, 0);
                    }
                });
            } catch (...) {
                if (currentCallback == nullptr) {
                    postCallback(callback, kCodeUnavailable, 0, 0);
                } else {
                    deliverCurrent(kCodeUnavailable, 0, 0);
                }
            }
        });
    } catch (...) {
        postCallback(callback, kCodeUnavailable, 0, 0);
    }
}

void location_cancel(void) {
    try {
        invokeAsync([] {
            currentGeneration += 1;
            if (currentCallback != nullptr) {
                deliverCurrent(kCodeUnavailable, 0, 0);
            }
            if (positionOp) {
                positionOp.Cancel();
                positionOp = nullptr;
            }
        });
    } catch (...) {
    }
}
