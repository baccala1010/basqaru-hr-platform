package kz.whosnext.hr.documentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
public class NcaNodeService {

    @Value("${app.ncanode.url}")
    private String ncaNodeUrl;

    @Value("${app.ecp.company-key-path}")
    private String companyKeyPath;

    @Value("${app.ecp.company-key-password}")
    private String companyKeyPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── XML signing (legacy, for XML-based flows) ───────────────────────────

    public Map<String, Object> xmlSignWithCompanyKey(String xml) {
        String keyBase64 = loadKeyAsBase64(companyKeyPath);
        return xmlSign(xml, keyBase64, companyKeyPassword);
    }

    public Map<String, Object> xmlSign(String xml, String keyBase64, String password) {
        try {
            Map<String, Object> signer = new HashMap<>();
            signer.put("key", keyBase64);
            signer.put("password", password);

            Map<String, Object> body = new HashMap<>();
            body.put("xml", xml);
            body.put("signers", List.of(signer));

            HttpEntity<Map<String, Object>> request = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/xml/sign", request, Map.class);

            log.info("NCANode XML sign: status={}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка XML-подписания через NCANode: {}", e.getMessage());
            throw new RuntimeException("Ошибка XML-подписания через NCANode", e);
        }
    }

    public Map<String, Object> xmlVerify(String signedXml) {
        try {
            Map<String, Object> body = Map.of("xml", signedXml);
            HttpEntity<Map<String, Object>> request = jsonEntity(body);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/xml/verify", request, Map.class);

            log.info("NCANode XML verify: status={}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка верификации XML через NCANode: {}", e.getMessage());
            throw new RuntimeException("Ошибка верификации XML через NCANode", e);
        }
    }

    // ── CMS (PKCS#7) signing for PDF documents ─────────────────────────────

    /**
     * Signs raw data (e.g. PDF content hash or full PDF) with the company key
     * using PKCS#7/CMS detached signature via NCANode.
     *
     * @param data raw bytes to sign (typically the SHA-256 hash of the PDF)
     * @return NCANode response containing 'cms' field with the base64-encoded CMS
     */
    public Map<String, Object> cmsSignWithCompanyKey(byte[] data) {
        String keyBase64 = loadKeyAsBase64(companyKeyPath);
        return cmsSign(data, keyBase64, companyKeyPassword);
    }

    public Map<String, Object> cmsSign(byte[] data, String keyBase64, String password) {
        try {
            Map<String, Object> signer = new HashMap<>();
            signer.put("key", keyBase64);
            signer.put("password", password);

            Map<String, Object> body = new HashMap<>();
            body.put("data", Base64.getEncoder().encodeToString(data));
            body.put("signers", List.of(signer));

            HttpEntity<Map<String, Object>> request = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/cms/sign", request, Map.class);

            log.info("NCANode CMS sign: status={}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка CMS-подписания через NCANode: {}", e.getMessage());
            throw new RuntimeException("Ошибка CMS-подписания через NCANode", e);
        }
    }

    /**
     * Signs a SHA-256 hash of the document content with company key via CMS.
     * This produces a PKCS#7 detached signature that can be verified independently
     * or embedded into the PDF.
     */
    public Map<String, Object> signDocumentHashWithCompanyKey(byte[] documentBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] documentHash = digest.digest(documentBytes);
            return cmsSignWithCompanyKey(documentHash);
        } catch (Exception e) {
            log.error("Ошибка подписания хеша документа: {}", e.getMessage());
            throw new RuntimeException("Ошибка подписания документа", e);
        }
    }

    public Map<String, Object> cmsVerify(String cmsBase64) {
        try {
            Map<String, Object> body = Map.of("cms", cmsBase64);
            HttpEntity<Map<String, Object>> request = jsonEntity(body);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/cms/verify", request, Map.class);

            log.info("NCANode CMS verify: status={}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка верификации CMS через NCANode: {}", e.getMessage());
            throw new RuntimeException("Ошибка верификации CMS через NCANode", e);
        }
    }

    // ── TSP (Timestamp) ────────────────────────────────────────────────────

    /**
     * Gets a trusted timestamp from NCANode's TSP service.
     * This provides evidence that the document existed at a point in time.
     */
    public Map<String, Object> getTimestamp(byte[] data) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("data", Base64.getEncoder().encodeToString(data));

            HttpEntity<Map<String, Object>> request = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/ts", request, Map.class);

            log.info("NCANode TSP timestamp: status={}", response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка получения TSP timestamp: {}", e.getMessage());
            throw new RuntimeException("Ошибка получения TSP timestamp", e);
        }
    }

    /**
     * Verifies the certificate status via NCANode OCSP/CRL check.
     * Returns certificate validity information including not-revoked status.
     */
    public Map<String, Object> checkCertificateStatus(String keyBase64) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("key", keyBase64);

            HttpEntity<Map<String, Object>> request = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ncaNodeUrl + "/key/info", request, Map.class);

            Map<String, Object> result = response.getBody();
            log.info("NCANode certificate check: status={}", response.getStatusCode());
            return result;
        } catch (Exception e) {
            log.error("Ошибка проверки сертификата: {}", e.getMessage());
            throw new RuntimeException("Ошибка проверки сертификата", e);
        }
    }

    // ── Counter-signature (XML) ────────────────────────────────────────────

    public String counterSignWithCompanyKey(String candidateSignedXml) {
        return counterSignWithCompanyKey(candidateSignedXml, false);
    }

    public String counterSignWithCompanyKey(String candidateSignedXml, boolean skipCandidateVerify) {
        log.info("Контрподпись компании: добавляем ЭЦП директора (XML), skipVerify={}", skipCandidateVerify);

        if (!skipCandidateVerify) {
            Map<String, Object> verifyResult = xmlVerify(candidateSignedXml);
            Boolean valid = (Boolean) verifyResult.get("valid");
            if (valid == null || !valid) {
                throw new RuntimeException("Подпись кандидата невалидна, контрподпись невозможна");
            }
            log.info("Подпись кандидата верифицирована успешно");
        } else {
            log.warn("Верификация подписи кандидата пропущена (режим разработки)");
        }

        Map<String, Object> signResult = xmlSignWithCompanyKey(candidateSignedXml);
        String counterSignedXml = (String) signResult.get("xml");
        if (counterSignedXml == null) {
            throw new RuntimeException("NCANode не вернул подписанный XML");
        }
        log.info("Контрподпись компании выполнена успешно");
        return counterSignedXml;
    }

    private String loadKeyAsBase64(String keyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Path.of(keyPath));
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (IOException e) {
            log.error("Не удалось загрузить p12-ключ из {}: {}", keyPath, e.getMessage());
            throw new RuntimeException("Не удалось загрузить ключ ЭЦП компании", e);
        }
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
