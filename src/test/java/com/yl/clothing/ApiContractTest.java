package com.yl.clothing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Public JSON contracts captured before the layered refactor. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired com.yl.clothing.mapper.OrderMapper orders;
    @Autowired com.yl.clothing.service.SimulationCoordinator coordinator;

    @BeforeEach void testDatabaseOnly() {
        SimulationTestData.reset(db, coordinator);
    }

    @AfterEach void resetSimulationPreferences() { SimulationTestData.reset(db, coordinator); }

    private JsonNode data(MvcResult result) throws Exception {
        JsonNode root = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        fields(root, "data");
        return root.get("data");
    }

    private JsonNode read(String path) throws Exception {
        return data(mvc.perform(get(path)).andExpect(status().isOk()).andReturn());
    }

    private void fields(JsonNode node, String... expected) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    private void money(JsonNode value) {
        assertThat(value.isTextual()).isTrue();
        assertThat(value.asText()).matches("[0-9]+\\.[0-9]{2}");
    }

    private void timestamp(JsonNode value) {
        assertThat(value.isTextual()).isTrue();
        assertThat(value.asText()).endsWith("+08:00");
        java.time.OffsetDateTime.parse(value.asText());
    }

    @Test void productListingAndNestedImageContracts() throws Exception {
        JsonNode page = read("/api/products?pageSize=1");
        fields(page, "items", "total", "page", "pageSize");
        assertThat(page.get("total").isIntegralNumber()).isTrue();
        JsonNode product = page.get("items").get(0);
        String[] productFields = {"id", "code", "name", "description", "categoryId", "categoryName", "createdAt",
                "colorCount", "minPrice", "maxPrice", "onSaleCount", "listingCount", "thumbnail"};
        fields(product, productFields);
        money(product.get("minPrice"));
        timestamp(product.get("createdAt"));
        JsonNode detail = read("/api/products/1");
        Set<String> detailFields = new HashSet<>(Set.of(productFields));
        detailFields.add("colors");
        fields(detail, detailFields.toArray(String[]::new));
        JsonNode color = detail.get("colors").get(0);
        fields(color, "id", "productId", "name", "hex1", "hex2", "images");
        assertThat(color.get("hex2").isNull()).isTrue();
        fields(color.get("images").get(0), "id", "colorId", "url", "sortOrder");
        JsonNode listing = read("/api/products/1/listings?size=XXL&channelId=1&pageSize=1").get("items").get(0);
        fields(listing, "id", "skuId", "channelId", "channelName", "price", "onSale", "size", "colorId", "colorName", "hex1", "hex2", "productId");
        assertThat(listing.get("onSale").isBoolean()).isTrue();
        assertThat(listing.get("size").asText()).isEqualTo("XXL");
        money(listing.get("price"));
    }

    @Test void mutationResponsesAndNullableFieldsStayCompatible() throws Exception {
        JsonNode product = data(mvc.perform(post("/api/products").contentType("application/json")
                .content("{\"code\":\"CONTRACT-EMPTY\",\"name\":\"接口契约\",\"categoryId\":1}"))
                .andExpect(status().isCreated()).andReturn());
        long id = product.get("id").asLong();
        for (String field : new String[]{"thumbnail", "minPrice", "maxPrice"}) assertThat(product.get(field).isNull()).isTrue();
        assertThat(product.get("colors").isArray()).isTrue();
        assertThat(product.get("colors").isEmpty()).isTrue();
        JsonNode patched = data(mvc.perform(patch("/api/products/" + id).contentType("application/json")
                .content("{\"description\":\"只改描述\"}")).andExpect(status().isOk()).andReturn());
        assertThat(patched.get("name")).isEqualTo(product.get("name"));
        JsonNode color = data(mvc.perform(post("/api/products/" + id + "/colors").contentType("application/json")
                .content("{\"name\":\"双色\",\"hex1\":\"#FFFFFF\",\"hex2\":\"#000000\",\"initialPrice\":\"12.30\"}"))
                .andExpect(status().isCreated()).andReturn());
        fields(color, "id", "productId", "name", "hex1", "hex2");
        JsonNode cleared = data(mvc.perform(patch("/api/product-colors/" + color.get("id").asLong()).contentType("application/json")
                .content("{\"name\":\"单色\",\"hex1\":\"#FFFFFF\",\"hex2\":null}"))
                .andExpect(status().isOk()).andReturn());
        assertThat(cleared.get("hex2").isNull()).isTrue();
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test void catalogUserAndSimulatorContracts() throws Exception {
        fields(read("/api/categories").get(0), "id", "name", "productCount");
        JsonNode category = data(mvc.perform(post("/api/categories").contentType("application/json")
                .content("{\"name\":\"契约品类\"}")).andExpect(status().isCreated()).andReturn());
        fields(category, "id", "name");
        fields(read("/api/channels").get(0), "id", "name", "code", "userCount", "orderCount", "productCount", "onSaleCount");
        JsonNode user = read("/api/users/1");
        fields(user, "id", "phone", "nickname", "gender", "birthday", "createdAt", "channelCount", "orderCount");
        timestamp(user.get("createdAt"));
        fields(read("/api/users/1/channels").get(0), "id", "name", "code", "orderCount");
        String input = "{\"phone\":\"" + user.get("phone").asText() + "\",\"nickname\":\"契约用户\",\"gender\":\"UNKNOWN\",\"birthday\":null}";
        JsonNode cleared = data(mvc.perform(patch("/api/users/1").contentType("application/json").content(input))
                .andExpect(status().isOk()).andReturn());
        assertThat(cleared.get("birthday").isNull()).isTrue();
        JsonNode dated = data(mvc.perform(patch("/api/users/1").contentType("application/json")
                .content(input.replace("null", "\"2000-02-29\""))).andExpect(status().isOk()).andReturn());
        assertThat(dated.get("birthday").asText()).isEqualTo("2000-02-29");
        JsonNode simulator = read("/api/order-simulator");
        fields(simulator, "enabled", "retainData", "state", "generatedCount", "updatedCount", "tickCount", "intervalMs", "lastTickAt", "blockedReason", "error");
        assertThat(simulator.get("enabled").isBoolean()).isTrue();
        assertThat(simulator.get("retainData").isBoolean()).isTrue();
        assertThat(simulator.get("intervalMs").asInt()).isEqualTo(500);
        assertThat(simulator.get("lastTickAt").isNull()).isTrue();
        JsonNode dataSimulator = read("/api/data-simulator");
        fields(dataSimulator, "enabled", "retainData", "state", "generatedCount", "generatedUserCount", "updatedCount", "tickCount",
                "intervalMs", "userIntervalMs", "lastTickAt", "blockedReason", "error");
        assertThat(dataSimulator.get("enabled").isBoolean()).isTrue();
        assertThat(dataSimulator.get("retainData").isBoolean()).isTrue();
        assertThat(dataSimulator.get("generatedUserCount").isIntegralNumber()).isTrue();
        assertThat(dataSimulator.get("intervalMs").asInt()).isEqualTo(500);
        assertThat(dataSimulator.get("userIntervalMs").asInt()).isEqualTo(15_000);
        mvc.perform(put("/api/data-simulator").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/data-simulator").contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test void simulatorRetentionPatchRequiresABooleanAndReturnsTheSameStatusContract() throws Exception {
        for (String path : new String[]{"/api/order-simulator", "/api/data-simulator"}) {
            JsonNode before = read(path);
            assertThat(before.get("retainData").asBoolean()).isFalse();
            for (String invalid : new String[]{"{}", "{\"retainData\":null}", "{\"retainData\":1}",
                    "{\"retainData\":\"true\"}"}) {
                mvc.perform(patch(path).contentType("application/json").content(invalid))
                        .andExpect(status().isBadRequest());
            }
            JsonNode updated = data(mvc.perform(patch(path).contentType("application/json")
                    .content("{\"retainData\":true}"))
                    .andExpect(status().isOk()).andReturn());
            Set<String> expected = new HashSet<>();
            before.fieldNames().forEachRemaining(expected::add);
            fields(updated, expected.toArray(String[]::new));
            assertThat(updated.get("enabled").asBoolean()).isFalse();
            assertThat(updated.get("retainData").asBoolean()).isTrue();
            assertThat(read(path)).isEqualTo(updated);
            mvc.perform(put(path).contentType("application/json").content("{\"enabled\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.retainData").value(true));
            mvc.perform(patch(path).contentType("application/json").content("{\"retainData\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.retainData").value(false));
        }
    }

    @Test void orderSnapshotAndErrorContracts() throws Exception {
        assertThat(db.queryForObject("SELECT DATABASE()", String.class)).isEqualToIgnoringCase("YL_260924_test");
        db.update("""
                INSERT INTO sales_order(order_no,user_id,listing_id,channel_id,product_code,product_name,color_name,hex1,hex2,size,channel_name,unit_price,quantity,total_price,status)
                VALUES('CONTRACT-ORDER',1,1,1,'YL-2601','快照款','云白','#FFFFFF',NULL,'S','抖音',12.30,2,24.60,'PENDING_PAYMENT')
                """);
        Long id = db.queryForObject("SELECT id FROM sales_order WHERE order_no='CONTRACT-ORDER'", Long.class);
        JsonNode order = read("/api/orders/" + id);
        fields(order, "id", "orderNo", "userId", "phone", "nickname", "listingId", "channelId", "channelName", "productCode",
                "productName", "colorName", "hex1", "hex2", "size", "unitPrice", "quantity", "totalPrice", "status", "createdAt", "updatedAt");
        money(order.get("unitPrice"));
        money(order.get("totalPrice"));
        timestamp(order.get("createdAt"));
        timestamp(order.get("updatedAt"));
        assertThat(order.get("hex2").isNull()).isTrue();
        assertThat(read("/api/users/1/orders?channelId=1&status=PENDING_PAYMENT").get("items").get(0)).isEqualTo(order);
        orders.updateStatus(id, "PENDING_PAYMENT", "SHIPPED", java.time.LocalDateTime.now());
        JsonNode shipped = read("/api/orders/" + id);
        assertThat(shipped.get("status").asText()).isEqualTo("SHIPPED");
        assertThat(read("/api/users/1/orders?channelId=1&status=SHIPPED").get("items").get(0)).isEqualTo(shipped);
        assertThat(read("/api/users/1/orders?status=PENDING_PAYMENT").get("total").asInt()).isZero();
        MvcResult invalid = mvc.perform(post("/api/categories").contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andReturn();
        JsonNode error = json.readTree(invalid.getResponse().getContentAsString(StandardCharsets.UTF_8));
        fields(error, "code", "message", "fieldErrors");
        assertThat(error.get("code").asText()).isEqualTo("HTTP_400");
        assertThat(error.get("fieldErrors").has("name")).isTrue();
        mvc.perform(get("/api/products/99999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HTTP_404"));
    }
}
