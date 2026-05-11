package com.zzp.pvms.media.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScanDirectoriesRequest {
    private List<Long> directoryIds;
}
