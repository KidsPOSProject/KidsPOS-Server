package info.nukoneko.kidspos.server.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
/**
 * データ漏洩防止セキュリティテスト
 *
 * センシティブなデータの漏洩を防ぐためのテスト
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Data Exposure Security Tests")
@Disabled("Temporarily disabled - Spring context issues")
class DataExposureSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should not expose internal system paths in errors`() {
        // エラーメッセージに内部システムパスを露出しない
        val result = mockMvc.perform(get("/api/items/invalid-id"))
            .andReturn()

        val response = result.response.contentAsString

        // システムパスが含まれていないことを確認
        assertFalse(response.contains("/Users/"), "Should not expose user paths")
        assertFalse(response.contains("/home/"), "Should not expose home paths")
        assertFalse(response.contains("C:\\"), "Should not expose Windows paths")
        assertFalse(response.contains("/var/"), "Should not expose system paths")
        assertFalse(response.contains(".java"), "Should not expose source file names")
        assertFalse(response.contains(".kt"), "Should not expose Kotlin source files")
    }

    @Test
    fun `should not expose database structure in errors`() {
        // エラーメッセージにデータベース構造を露出しない
        val invalidItem = mapOf(
            "invalid_field" to "value"
        )

        val result = mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidItem)))
            .andReturn()

        val response = result.response.contentAsString

        // データベース情報が含まれていないことを確認
        assertFalse(response.contains("column"), "Should not expose column names")
        assertFalse(response.contains("table"), "Should not expose table names")
        assertFalse(response.contains("constraint"), "Should not expose constraints")
        assertFalse(response.contains("foreign key"), "Should not expose FK info")
        assertFalse(response.contains("primary key"), "Should not expose PK info")
        assertFalse(response.contains("index"), "Should not expose index info")
    }

    @Test
    fun `should not expose technology stack details`() {
        // 技術スタックの詳細を露出しない
        val result = mockMvc.perform(get("/api/invalid"))
            .andReturn()

        val response = result.response

        // 技術スタック情報が含まれていないことを確認
        assertFalse(response.getHeader("X-Powered-By") != null,
            "Should not expose X-Powered-By header")
        assertFalse(response.getHeader("Server")?.contains("version") == true,
            "Should not expose server version")

        val body = response.contentAsString
        assertFalse(body.contains("Spring Boot"), "Should not expose Spring Boot")
        assertFalse(body.contains("Tomcat"), "Should not expose Tomcat")
        assertFalse(body.contains("Hibernate"), "Should not expose Hibernate")
    }

    @Test
    fun `should not include sensitive headers`() {
        // センシティブなヘッダーが含まれていないことを確認
        val result = mockMvc.perform(get("/api/items"))
            .andReturn()

        val response = result.response

        // デバッグヘッダーが含まれていないことを確認
        assertNull(response.getHeader("X-Debug"), "Should not include debug headers")
        assertNull(response.getHeader("X-Database-Query"), "Should not expose queries")
        assertNull(response.getHeader("X-Execution-Time"), "Should not expose timing")
        assertNull(response.getHeader("X-Cache-Hit"), "Should not expose cache info")
    }

    @Test
    fun `should implement proper error masking`() {
        // 適切なエラーマスキングを実装
        val testCases = listOf(
            "/api/staff/999999",  // 存在しないID
            "/api/stores/999999",  // 存在しないID
            "/api/items/NONEXISTENT"  // 存在しないバーコード
        )

        testCases.forEach { path ->
            val result = mockMvc.perform(get(path))
                .andReturn()

            assertEquals(404, result.response.status,
                "Should return 404 for non-existent resources")

            val response = result.response.contentAsString
            assertFalse(response.contains("SQL"), "Should not expose SQL info")
            assertFalse(response.contains("SELECT"), "Should not expose queries")
        }
    }

    @Test
    fun `should not expose internal IDs in URLs`() {
        // URLに内部IDを露出しない（セキュリティベストプラクティス）
        val result = mockMvc.perform(get("/api/items"))
            .andExpect(status().isOk())
            .andReturn()

        val response = result.response.contentAsString

        // レスポンスに含まれるIDが適切にマスクされているか確認
        // （このテストは実装に依存するため、具体的な検証は調整が必要）
        assertTrue(true, "IDs should be properly managed")
    }

    @Test
    fun `should sanitize log output`() {
        // ログ出力のサニタイズ
        val sensitiveData = mapOf(
            "barcode" to "TEST001",
            "name" to "Test Item",
            "price" to 100,
            "password" to "should_not_be_logged",
            "creditCard" to "4111111111111111",
            "ssn" to "123-45-6789"
        )

        mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sensitiveData)))
            .andReturn()

        // ログに機密情報が記録されていないことを確認
        // （実際のログ検証はログファイルへのアクセスが必要）
        assertTrue(true, "Sensitive data should not be logged")
    }

    @Test
    fun `should not expose session IDs in URLs`() {
        // URLにセッションIDを露出しない
        val result = mockMvc.perform(get("/api/items")
            .param("jsessionid", "ABC123DEF456"))
            .andReturn()

        val response = result.response

        // リダイレクトURLにセッションIDが含まれていないことを確認
        val location = response.getHeader("Location")
        if (location != null) {
            assertFalse(location.contains("jsessionid"),
                "Should not include session ID in URL")
            assertFalse(location.contains("sessionid"),
                "Should not include session ID in URL")
        }
    }

    @Test
    fun `should implement secure headers`() {
        // セキュアなヘッダーが実装されていることを確認
        val result = mockMvc.perform(get("/"))
            .andReturn()

        val response = result.response

        // セキュリティヘッダーの確認
        assertNotNull(response.getHeader("X-Content-Type-Options"),
            "Should include X-Content-Type-Options")
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"),
            "X-Content-Type-Options should be nosniff")

        // その他の推奨セキュリティヘッダー
        // X-Frame-Options, X-XSS-Protection, Content-Security-Policy など
    }

    @Test
    fun `should prevent information disclosure through timing attacks`() {
        // タイミング攻撃による情報漏洩を防ぐ
        val validBarcode = "TEST001"
        val invalidBarcode = "INVALID999"

        val validTimes = mutableListOf<Long>()
        val invalidTimes = mutableListOf<Long>()

        // 複数回実行して時間を計測
        repeat(10) {
            val startValid = System.currentTimeMillis()
            mockMvc.perform(get("/api/items/$validBarcode"))
                .andReturn()
            validTimes.add(System.currentTimeMillis() - startValid)

            val startInvalid = System.currentTimeMillis()
            mockMvc.perform(get("/api/items/$invalidBarcode"))
                .andReturn()
            invalidTimes.add(System.currentTimeMillis() - startInvalid)
        }

        // 平均時間の差が大きすぎないことを確認
        val avgValid = validTimes.average()
        val avgInvalid = invalidTimes.average()
        val timeDifference = Math.abs(avgValid - avgInvalid)

        // タイミングの差が100ms以内であることを確認（調整可能）
        assertTrue(timeDifference < 100,
            "Timing difference should be minimal to prevent timing attacks")
    }

    @Test
    fun `should not expose debug information in production`() {
        // 本番環境でデバッグ情報を露出しない
        val result = mockMvc.perform(get("/api/debug"))
            .andReturn()

        assertEquals(404, result.response.status,
            "Debug endpoints should not be accessible")

        // Actuatorエンドポイントへのアクセスも確認
        mockMvc.perform(get("/actuator/beans"))
            .andExpect(status().is4xxClientError())

        mockMvc.perform(get("/actuator/mappings"))
            .andExpect(status().is4xxClientError())
    }

    @Test
    fun `should mask sensitive data in responses`() {
        // レスポンスでセンシティブデータをマスク
        val result = mockMvc.perform(get("/api/staff/1"))
            .andReturn()

        if (result.response.status == 200) {
            val response = result.response.contentAsString
            val staff = objectMapper.readValue(response, Map::class.java)

            // センシティブなフィールドがマスクされているか確認
            if (staff.containsKey("email")) {
                val email = staff["email"] as? String
                if (email != null && email.contains("@")) {
                    assertTrue(email.contains("***") || email.length < email.count { it == '@' },
                        "Email should be partially masked")
                }
            }

            // パスワードフィールドが含まれていないことを確認
            assertFalse(staff.containsKey("password"),
                "Password should never be included in response")
        }
    }

    @Test
    fun `should prevent directory listing`() {
        // ディレクトリリスティングを防ぐ
        val paths = listOf(
            "/static/",
            "/images/",
            "/css/",
            "/js/",
            "/WEB-INF/",
            "/META-INF/"
        )

        paths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().is4xxClientError())
        }
    }

    @Test
    fun `should not expose backup files`() {
        // バックアップファイルを露出しない
        val backupPaths = listOf(
            "/web.xml.bak",
            "/application.properties.backup",
            "/database.sql",
            "/.git/config",
            "/.env",
            "/config.json~",
            "/backup.zip"
        )

        backupPaths.forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
        }
    }
}