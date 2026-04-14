package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import java.util.Map;

public interface MaintenanceService {
    ResponseData<Map<String, Long>> performCleanup();
}
