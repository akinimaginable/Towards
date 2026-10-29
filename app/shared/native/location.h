#ifndef TOWARDS_LOCATION_H
#define TOWARDS_LOCATION_H

#ifdef __cplusplus
extern "C" {
#endif

#if defined(_WIN32)
#define TOWARDS_LOCATION_API __declspec(dllexport)
#else
#define TOWARDS_LOCATION_API
#endif

/* Authorization: 0 not determined, 1 denied, 2 granted, 3 unavailable. */
/* Callback code: 0 success, 1 denied, 2 timeout or unavailable. */
typedef void (*towards_location_callback)(int code, double latitude, double longitude);

TOWARDS_LOCATION_API int location_host_declares_location_usage(void);
TOWARDS_LOCATION_API int location_authorization_status(void);
TOWARDS_LOCATION_API int location_last_known(double *latitude, double *longitude);
TOWARDS_LOCATION_API void location_request_permission(towards_location_callback callback);
TOWARDS_LOCATION_API void location_request_current(towards_location_callback callback);
TOWARDS_LOCATION_API void location_cancel(void);

#ifdef __cplusplus
}
#endif

#endif
