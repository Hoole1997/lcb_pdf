package com.documentpro.office.business.fileviewer.utils

import androidx.core.content.FileProvider

/**
 * Custom FileProvider subclass to avoid manifest merger conflicts
 * with other FileProvider declarations (e.g., overview.fileprovider).
 */
class PdfFileProvider : FileProvider()
