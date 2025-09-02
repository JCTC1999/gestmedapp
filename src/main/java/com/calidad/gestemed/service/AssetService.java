package com.calidad.gestemed.service;

// service/AssetService.java

import com.calidad.gestemed.domain.Asset;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AssetService {
    Asset create(Asset a, List<MultipartFile> photos, String createdBy);
    List<Asset> list();
}
