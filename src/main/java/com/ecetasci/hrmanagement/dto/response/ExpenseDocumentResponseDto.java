package com.ecetasci.hrmanagement.dto.response;

public record ExpenseDocumentResponseDto( Long id,
                                          String fileName,
                                          String filePath,
                                          String fileType,
                                          Long expenseId) {
}
