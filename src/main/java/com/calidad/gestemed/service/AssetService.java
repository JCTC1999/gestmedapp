package com.calidad.gestemed.service;

// service/AssetService.java

import com.calidad.gestemed.domain.Asset;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AssetService {
    Asset create(Asset a, String createdBy);
    List<Asset> list();
}
