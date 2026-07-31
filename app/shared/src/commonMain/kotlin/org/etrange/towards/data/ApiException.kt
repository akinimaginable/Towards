package org.etrange.towards.data

class ApiException(
    val statusCode: Int,
    message: String,
    val correlationId: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
