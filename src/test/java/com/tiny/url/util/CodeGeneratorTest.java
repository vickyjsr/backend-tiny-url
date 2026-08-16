package com.tiny.url.util;

import com.tiny.url.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeGeneratorTest {

    @Mock
    private UrlRepository urlRepository;

    private CodeGenerator codeGenerator;

    @BeforeEach
    void setUp() throws Exception {
        codeGenerator = new CodeGenerator(urlRepository);
        when(urlRepository.findByTinyUrl(anyString())).thenReturn(null);
    }

    @Test
    void generatesFixedLengthCode() {
        String code = codeGenerator.generateUniqueCode("https://example.com/path");
        assertNotNull(code);
        assertEquals(Constants.CODE_LENGTH, code.length());
    }

    @Test
    void isDeterministicForSameInputWhenAvailable() {
        String first = codeGenerator.generateUniqueCode("https://slashurl.com/same");
        String second = codeGenerator.generateUniqueCode("https://slashurl.com/same");
        assertEquals(first, second);
    }
}
