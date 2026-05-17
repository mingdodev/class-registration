package com.example.classregistration.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CursorPage<T> {

    private final List<T> content;
    private final String nextCursor; // 마지막 항목의 createdAt (ISO-8601 형식)
    private final boolean hasNext;
}
