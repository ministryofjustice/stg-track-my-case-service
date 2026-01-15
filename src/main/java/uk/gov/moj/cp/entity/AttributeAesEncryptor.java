package uk.gov.moj.cp.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import uk.gov.moj.cp.config.SecretKeyLoader;
import uk.gov.moj.cp.util.CryptoUtils;

@Converter
@RequiredArgsConstructor
public class AttributeAesEncryptor implements AttributeConverter<String, String> {

    private final SecretKeyLoader encryptionSecret;

    @Override
    public String convertToDatabaseColumn(final String attribute) {
        return CryptoUtils.aesEncrypt(attribute, this.encryptionSecret.getAesSecretKeySpec());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return CryptoUtils.aesDecrypt(dbData, this.encryptionSecret.getAesSecretKeySpec());
    }
}
