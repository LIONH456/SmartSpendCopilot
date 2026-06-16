package com.smartspend.copilot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Generic paginated response wrapper")
public class PaginatedResponse<T> {
    @Schema(description = "List of content items on the current page")
    private List<T> content;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "10")
    private int size;

    @Schema(description = "Total number of elements across all pages", example = "42")
    private long totalElements;

    @Schema(description = "Total number of available pages", example = "5")
    private int totalPages;

    @Schema(description = "Indicates if this is the last page", example = "false")
    private boolean last;
}
