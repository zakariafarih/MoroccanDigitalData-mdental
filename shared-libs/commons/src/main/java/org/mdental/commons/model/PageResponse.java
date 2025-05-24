package org.mdental.commons.model;

import java.util.List;

public record PageResponse<T>(
List<T> items,
int page,
int size,
long totalElements,
int totalPages
) { }