package com.retheviper.file.transporter.service

sealed class FileStorageException(
    message: String
) : RuntimeException(message)

class InvalidPathException(
    message: String = "Invalid target path"
) : FileStorageException(message)

class TargetDirectoryNotFoundException(
    message: String = "Target directory not found."
) : FileStorageException(message)

class DirectoryNotFoundException(
    message: String = "Directory not found."
) : FileStorageException(message)

class FileNotFoundException(
    message: String = "File not found."
) : FileStorageException(message)

class TargetNotDirectoryException(
    message: String = "Target is not a directory."
) : FileStorageException(message)
