package uk.gov.moj.cp.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import uk.gov.moj.cp.config.SecretKeyLoader;
import uk.gov.moj.cp.util.CryptoUtils;

@Converter
@RequiredArgsConstructor
public class AttributeHmacEncryptor implements AttributeConverter<String, String> {

    private final SecretKeyLoader encryptionSecret;

    @Override
    public String convertToDatabaseColumn(final String attribute) {
        return CryptoUtils.hmacEncrypt(attribute, this.encryptionSecret.getHmacEncryptionSecret());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return null;
    }
}
