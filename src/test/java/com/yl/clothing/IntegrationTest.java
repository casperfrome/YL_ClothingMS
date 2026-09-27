package com.yl.clothing;
import com.fasterxml.jackson.databind.*;
import com.yl.clothing.enums.OrderStatus;
import com.yl.clothing.service.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired OrderSimulatorService simulator;
    @Autowired DataSimulatorService dataSimulator;
    @Autowired SimulationBatchService batches;
    @Autowired SimulationCoordinator coordinator;
    @BeforeEach void before(){SimulationTestData.reset(db, coordinator);}
    @AfterEach void after(){SimulationTestData.reset(db, coordinator);}
    JsonNode body(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");}
    JsonNode create(String path,String content)throws Exception{return body(mvc.perform(post(path).contentType("application/json").content(content)).andExpect(status().isCreated()).andReturn());}
    @Test void seededDataAndFilters()throws Exception{
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product WHERE deleted=0",Integer.class)).isEqualTo(6);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_color WHERE deleted=0",Integer.class)).isEqualTo(12);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM listing",Integer.class)).isEqualTo(240);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_image",Integer.class)).isEqualTo(24);
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(6));
        mvc.perform(get("/api/products/1/listings")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(40));
        mvc.perform(get("/api/products/1/listings").param("size","XXL").param("channelId","1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(get("/api/products/1/listings").param("size","XXXL")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("pageSize","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(12));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"));
    }
    @Test @Transactional void productColorAndAtomicListingEdits()throws Exception{
        long id=create("/api/products","{\"code\":\"TEST-NEW\",\"name\":\"测试款\",\"categoryId\":1,\"description\":\"中文描述\"}").path("id").asLong();
        long color=create("/api/products/"+id+"/colors","{\"name\":\"红黑色\",\"hex1\":\"#FF0000\",\"hex2\":\"#000000\",\"initialPrice\":\"199.99\"}").path("id").asLong();
        JsonNode rows=body(mvc.perform(get("/api/products/"+id+"/listings")).andExpect(status().isOk()).andReturn());
        assertThat(rows.path("total").asInt()).isEqualTo(20);
        long listing=rows.path("items").get(0).path("id").asLong();
        assertThat(rows.path("items").get(0).path("price").asText()).isEqualTo("199.99");
        assertThat(rows.path("items").get(0).path("onSale").asBoolean()).isFalse();
        mvc.perform(patch("/api/listings").contentType("application/json").content("{\"ids\":["+listing+",999999999],\"price\":\"300.00\"}")).andExpect(status().isNotFound());
        assertThat(db.queryForObject("SELECT price FROM listing WHERE id=?",BigDecimal.class,listing)).isEqualByComparingTo("199.99");
        mvc.perform(patch("/api/listings").contentType("application/json").content("{\"ids\":["+listing+"],\"price\":\"300.01\",\"onSale\":true}")).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT price FROM listing WHERE id=?",BigDecimal.class,listing)).isEqualByComparingTo("300.01");
        mvc.perform(patch("/api/listings/"+listing).contentType("application/json").content("{\"price\":\"-1\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/products/"+id).contentType("application/json").content("{\"description\":\"修改描述\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value("修改描述"));
        mvc.perform(delete("/api/product-colors/"+color)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/"+id+"/listings")).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(delete("/api/products/"+id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/"+id)).andExpect(status().isNotFound());
    }
    @Test @Transactional void categoriesAndUsersValidateUniqueness()throws Exception{
        long category=create("/api/categories","{\"name\":\"测试新品类\"}").path("id").asLong();
        mvc.perform(patch("/api/categories/"+category).contentType("application/json").content("{\"name\":\"测试改名\"}")).andExpect(status().isOk());
        mvc.perform(delete("/api/categories/"+category)).andExpect(status().isNoContent());
        long retired=create("/api/categories","{\"name\":\"已删除商品的品类\"}").path("id").asLong();
        long retiredProduct=create("/api/products","{\"code\":\"TEST-RETIRED\",\"name\":\"退役款\",\"categoryId\":"+retired+"}").path("id").asLong();
        mvc.perform(delete("/api/products/"+retiredProduct)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/"+retired)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/1")).andExpect(status().isConflict());
        mvc.perform(post("/api/categories").contentType("application/json").content("{\"name\":\"T恤\"}")).andExpect(status().isConflict());
        String phone=db.queryForObject("SELECT phone FROM customer WHERE id=2",String.class);
        mvc.perform(patch("/api/users/1").contentType("application/json").content("{\"phone\":\""+phone+"\",\"nickname\":\"重复\",\"gender\":\"MALE\",\"birthday\":null}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/users/1").contentType("application/json").content("{\"phone\":\"123\",\"nickname\":\"短号\",\"gender\":\"MALE\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/users/1").contentType("application/json").content("{\"phone\":\"00123456789\",\"nickname\":\"新昵称\",\"gender\":\"FEMALE\",\"birthday\":\"2000-02-29\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.phone").value("00123456789"));
        mvc.perform(get("/api/users").param("phone","00123456789")).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/users/1/orders").param("channelId","4")).andExpect(status().isBadRequest());
    }
    @Test @Transactional void uploadsValidateContentAndImageOwnership()throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",out);
        MockMultipartFile file=new MockMultipartFile("files","test.png","image/png",out.toByteArray());
        JsonNode images=body(mvc.perform(multipart("/api/product-colors/1/images").file(file)).andExpect(status().isCreated()).andReturn());
        assertThat(images.size()).isEqualTo(3);
        long id=images.get(2).path("id").asLong();
        String ids="["+id+","+images.get(0).path("id").asLong()+","+images.get(1).path("id").asLong()+"]";
        mvc.perform(put("/api/product-colors/1/images/order").contentType("application/json").content("{\"ids\":"+ids+"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id));
        mvc.perform(put("/api/product-colors/2/images/order").contentType("application/json").content("{\"ids\":"+ids+"}")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/product-colors/1/images").file(new MockMultipartFile("files","bad.png","image/png","not an image".getBytes()))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/images/"+id)).andExpect(status().isNoContent());
    }
    @Test @Transactional void generationRespectsRelationsAndSnapshots()throws Exception{
        long runId=batches.switchRun(null,"ORDERS_ONLY",false);
        for(int i=0;i<35;i++){var r=batches.generate(runId);assertThat(r.created()).isBetween(1,3);assertThat(r.updated()).isBetween(0,300);}
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order o LEFT JOIN user_channel uc ON uc.user_id=o.user_id AND uc.channel_id=o.channel_id WHERE uc.user_id IS NULL",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order o JOIN listing l ON l.id=o.listing_id WHERE l.on_sale=0",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order WHERE total_price != quantity*unit_price",Integer.class)).isZero();
        Map<String,Object> o=db.queryForMap("SELECT id,listing_id,unit_price FROM sales_order LIMIT 1");
        db.update("UPDATE listing SET price=price+10 WHERE id=?",o.get("listing_id"));
        db.update("UPDATE product SET deleted=1 WHERE id=(SELECT pc.product_id FROM product_color pc JOIN sku s ON s.color_id=pc.id JOIN listing l ON l.sku_id=s.id WHERE l.id=?)",o.get("listing_id"));
        mvc.perform(get("/api/orders/"+o.get("id"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.unitPrice").value(o.get("unit_price").toString()));
        assertThat(db.queryForList("SELECT DISTINCT status FROM sales_order",String.class)).containsExactly("PENDING_PAYMENT");
    }
    @Test void invalidSecondUploadRollsBackFirstImage()throws Exception{
        int before=db.queryForObject("SELECT COUNT(*) FROM product_image WHERE color_id=1",Integer.class);
        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",out);
        mvc.perform(multipart("/api/product-colors/1/images")
            .file(new MockMultipartFile("files","valid.png","image/png",out.toByteArray()))
            .file(new MockMultipartFile("files","bad.png","image/png","invalid".getBytes())))
            .andExpect(status().isBadRequest());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM product_image WHERE color_id=1",Integer.class)).isEqualTo(before);
    }
    @Test void scheduledToggleIsIdempotentAndStopPreventsFurtherWrites()throws Exception{
        try(var pool=Executors.newFixedThreadPool(6)){
            List<Future<?>> futures=new ArrayList<>();
            for(int i=0;i<6;i++)futures.add(pool.submit(()->simulator.setEnabled(true)));
            for(Future<?> f:futures)f.get(3,TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(5)).until(()->simulator.status().tickCount()>=4);
        var s=simulator.status();long count=s.generatedCount(),ticks=s.tickCount();
        assertThat(count).isBetween(ticks,ticks*3);
        simulator.setEnabled(true);
        assertThat(simulator.status().generatedCount()).isGreaterThanOrEqualTo(count);
        simulator.setEnabled(false);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order",Long.class)).isZero();
        Thread.sleep(800);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order",Long.class)).isZero();
    }
    @Test void waitsWhenNoSaleableCombinationThenRecovers(){
        List<Long> available=db.queryForList("SELECT id FROM listing WHERE on_sale=1",Long.class);
        try{
            db.update("UPDATE listing SET on_sale=0");
            simulator.setEnabled(true);
            await().atMost(Duration.ofSeconds(4)).until(()->"WAITING".equals(simulator.status().state()));
            assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order",Long.class)).isZero();
            db.update("UPDATE listing SET on_sale=1 WHERE id=?",available.getFirst());
            await().atMost(Duration.ofSeconds(4)).until(()->simulator.status().generatedCount()>0);
        }finally{
            simulator.setEnabled(false);
            db.update("UPDATE listing SET on_sale=0");
            for(long id:available)db.update("UPDATE listing SET on_sale=1 WHERE id=?",id);
        }
    }
    @Test void statusTransitionsPreserveTerminalStates(){
        var random=new Random(260924);
        for(int i=0;i<100;i++){
            assertThat(OrderStatus.PENDING_PAYMENT.next(random)).isIn(OrderStatus.PENDING_SHIPMENT,OrderStatus.CANCELLED);
            assertThat(OrderStatus.PENDING_SHIPMENT.next(random)).isEqualTo(OrderStatus.SHIPPED);
            assertThat(OrderStatus.SHIPPED.next(random)).isIn(OrderStatus.COMPLETED,OrderStatus.REFUNDED);
            for(var state:List.of(OrderStatus.CANCELLED,OrderStatus.REFUNDED,OrderStatus.COMPLETED))assertThat(state.next(random)).isEqualTo(state);
        }
    }
}
