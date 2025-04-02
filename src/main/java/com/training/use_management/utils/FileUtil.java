package com.training.use_management.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileUtil {

    @Value("${app.upload.max-file-size}")
    private String maxFileSizeConfig;

    public enum FileSizeUnit {
        KB(1024), MB(1024 * 1024), GB(1024 * 1024 * 1024);

        private final long factor;

        FileSizeUnit(long factor) {
            this.factor = factor;
        }

        public long getFactor() {
            return factor;
        }

        public static long convertToBytes(String size) {
            size = size.toUpperCase();
            for (FileSizeUnit unit : values()) {
                if (size.endsWith(unit.name())) {
                    return Long.parseLong(size.replace(unit.name(), "").trim()) * unit.getFactor();
                }
            }
            return Long.parseLong(size.trim());  // Default to bytes
        }
    }

    // Chuyển đổi dung lượng sang byte
    public long getMaxFileSizeInBytes() {
        return FileSizeUnit.convertToBytes(maxFileSizeConfig);
    }

    public boolean isFileSizeValid(long fileSize) {
        return fileSize <= getMaxFileSizeInBytes();
    }

    public String getMaxFileSizeConfig() {
        return maxFileSizeConfig;
    }
}