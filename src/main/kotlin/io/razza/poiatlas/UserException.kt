package io.razza.poiatlas

/**
 * Exception type for error messages meant to be shown to the user.
 *
 * @param message The message to display to the user.
 * @param innerException The original exception causing this error, if available.
 */
class UserException(message: String, innerException: Exception? = null) : RuntimeException(message, innerException)