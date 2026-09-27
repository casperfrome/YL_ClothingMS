-- 仅补充业务表及字段注释，保留 V1-V5 确定的字段定义和约束。
ALTER TABLE category
 COMMENT = '商品品类：款式使用的单层分类',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '品类主键ID',
 MODIFY COLUMN name VARCHAR(60) NOT NULL COMMENT '品类名称，全局唯一';

ALTER TABLE channel
 COMMENT = '销售渠道目录：抖音、淘宝、京东、拼多多',
 MODIFY COLUMN id BIGINT NOT NULL COMMENT '渠道主键ID，预置固定值',
 MODIFY COLUMN code VARCHAR(20) NOT NULL COMMENT '渠道编码：DOUYIN=抖音，TAOBAO=淘宝，JD=京东，PDD=拼多多；全局唯一',
 MODIFY COLUMN name VARCHAR(30) NOT NULL COMMENT '渠道名称，全局唯一';

ALTER TABLE product
 COMMENT = '商品款式（SPU）：统一维护款号、名称、描述和品类',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品款式主键ID',
 MODIFY COLUMN code VARCHAR(40) NOT NULL COMMENT '款号，全局唯一；逻辑删除后仍保留占用',
 MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '商品款式名称',
 MODIFY COLUMN description TEXT NOT NULL COMMENT '商品款式描述，同款各配色共用',
 MODIFY COLUMN category_id BIGINT NULL DEFAULT NULL COMMENT '品类ID，关联 category.id；已删除商品解除品类关联后为NULL',
 MODIFY COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标志：0=未删除，1=已删除',
 MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '商品款式创建时间，毫秒精度，业务时区为Asia/Shanghai';

ALTER TABLE product_color
 COMMENT = '商品配色：所属款式下的单色或双色组合',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品配色主键ID',
 MODIFY COLUMN product_id BIGINT NOT NULL COMMENT '所属商品款式ID，关联 product.id',
 MODIFY COLUMN name VARCHAR(40) NOT NULL COMMENT '配色名称',
 MODIFY COLUMN hex1 CHAR(7) NOT NULL COMMENT '第一颜色值，格式为#RRGGBB',
 MODIFY COLUMN hex2 CHAR(7) NULL DEFAULT NULL COMMENT '第二颜色值，格式为#RRGGBB；NULL表示单色',
 MODIFY COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '逻辑删除标志：0=未删除，1=已删除';

ALTER TABLE product_image
 COMMENT = '商品配色图片：保存图片访问路径及展示顺序',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品图片主键ID',
 MODIFY COLUMN color_id BIGINT NOT NULL COMMENT '所属配色ID，关联 product_color.id',
 MODIFY COLUMN url VARCHAR(255) NOT NULL COMMENT '图片访问路径，指向内置示例图片或上传文件，不存储图片二进制内容',
 MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '配色内图片排序序号，从0开始，数值越小越靠前';

ALTER TABLE sku
 COMMENT = '商品SKU：配色与尺码的唯一组合',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU主键ID',
 MODIFY COLUMN color_id BIGINT NOT NULL COMMENT '所属配色ID，关联 product_color.id',
 MODIFY COLUMN size ENUM('S','M','L','XL','XXL') NOT NULL COMMENT '尺码：S、M、L、XL、XXL；同一配色内唯一';

ALTER TABLE listing
 COMMENT = '渠道销售记录：SKU在各渠道的售价及上下架状态',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '渠道销售记录主键ID',
 MODIFY COLUMN sku_id BIGINT NOT NULL COMMENT 'SKU ID，关联 sku.id；与channel_id组成唯一销售记录',
 MODIFY COLUMN channel_id BIGINT NOT NULL COMMENT '销售渠道ID，关联 channel.id',
 MODIFY COLUMN price DECIMAL(10,2) NOT NULL COMMENT '当前销售单价，单位为元，保留两位小数且必须大于0',
 MODIFY COLUMN on_sale BOOLEAN NOT NULL DEFAULT FALSE COMMENT '上下架状态：0=下架，1=上架';

ALTER TABLE customer
 COMMENT = '用户资料：包含初始用户和模拟器生成的用户',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
 MODIFY COLUMN phone CHAR(11) NOT NULL COMMENT '11位手机号，全局唯一，按字符串保存以保留前导零',
 MODIFY COLUMN nickname VARCHAR(40) NOT NULL COMMENT '用户昵称',
 MODIFY COLUMN gender ENUM('UNKNOWN','MALE','FEMALE') NOT NULL DEFAULT 'UNKNOWN' COMMENT '性别：UNKNOWN=未知，MALE=男，FEMALE=女',
 MODIFY COLUMN birthday DATE NULL DEFAULT NULL COMMENT '用户生日；NULL表示未填写',
 MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '用户创建时间，毫秒精度，业务时区为Asia/Shanghai',
 MODIFY COLUMN simulated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '模拟器生成标志：0=非模拟器生成，1=模拟器生成；清理归属由simulation_run_id确定',
 MODIFY COLUMN simulation_run_id BIGINT NULL DEFAULT NULL COMMENT '生成批次ID，关联 simulation_run.id；NULL表示不归属模拟批次';

ALTER TABLE user_channel
 COMMENT = '用户渠道开通关系：用户与销售渠道的多对多关联',
 MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户ID，关联 customer.id；与channel_id组成联合主键',
 MODIFY COLUMN channel_id BIGINT NOT NULL COMMENT '已开通渠道ID，关联 channel.id；与user_id组成联合主键';

ALTER TABLE sales_order
 COMMENT = '销售订单：保存模拟订单、下单时商品价格快照及状态流转信息',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
 MODIFY COLUMN order_no VARCHAR(50) NOT NULL COMMENT '业务订单号，全局唯一',
 MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '下单用户ID，关联 customer.id',
 MODIFY COLUMN listing_id BIGINT NOT NULL COMMENT '下单销售记录ID，关联 listing.id',
 MODIFY COLUMN channel_id BIGINT NOT NULL COMMENT '下单渠道ID，关联 channel.id',
 MODIFY COLUMN product_code VARCHAR(40) NOT NULL COMMENT '下单时的商品款号快照，不随商品后续修改变化',
 MODIFY COLUMN product_name VARCHAR(100) NOT NULL COMMENT '下单时的商品名称快照，不随商品后续修改变化',
 MODIFY COLUMN color_name VARCHAR(40) NOT NULL COMMENT '下单时的配色名称快照',
 MODIFY COLUMN hex1 CHAR(7) NOT NULL COMMENT '下单时的第一颜色值快照，格式为#RRGGBB',
 MODIFY COLUMN hex2 CHAR(7) NULL DEFAULT NULL COMMENT '下单时的第二颜色值快照，格式为#RRGGBB；NULL表示单色',
 MODIFY COLUMN size VARCHAR(5) NOT NULL COMMENT '下单时的尺码快照：S、M、L、XL、XXL',
 MODIFY COLUMN channel_name VARCHAR(30) NOT NULL COMMENT '下单时的渠道名称快照',
 MODIFY COLUMN unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时的成交单价快照，单位为元，保留两位小数',
 MODIFY COLUMN quantity INT NOT NULL COMMENT '购买数量，取值为1至3件',
 MODIFY COLUMN total_price DECIMAL(12,2) NOT NULL COMMENT '订单总金额，单位为元，保留两位小数，等于成交单价乘以数量',
 MODIFY COLUMN status ENUM('PENDING_PAYMENT','CANCELLED','PENDING_SHIPMENT','REFUNDED','COMPLETED','SHIPPED') NOT NULL COMMENT '订单状态：PENDING_PAYMENT=待付款，CANCELLED=已取消，PENDING_SHIPMENT=待发货，REFUNDED=已退款，COMPLETED=已完成，SHIPPED=已发货',
 MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '订单创建时间，毫秒精度，业务时区为Asia/Shanghai',
 MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '订单状态最后更新时间，由应用维护，作为状态流转计时起点，业务时区为Asia/Shanghai',
 MODIFY COLUMN simulation_run_id BIGINT NULL DEFAULT NULL COMMENT '生成批次ID，关联 simulation_run.id，用于保留或清理订单；NULL表示不归属模拟批次';

ALTER TABLE simulation_settings
 COMMENT = '模拟器持久化设置：分别保存订单模式和数据模式的保留偏好',
 MODIFY COLUMN mode VARCHAR(20) NOT NULL COMMENT '模拟模式主键：ORDERS_ONLY=仅生成订单，FULL_DATA=生成用户、渠道关系及订单',
 MODIFY COLUMN retain_data BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否保留生成数据：0=停止后清理，1=停止后保留；运行时修改同步到当前批次，停止时修改用于下次生成';

ALTER TABLE simulation_run
 COMMENT = '模拟生成批次：记录数据归属、保留标志及批次起止时间',
 MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模拟生成批次主键ID',
 MODIFY COLUMN mode VARCHAR(20) NOT NULL COMMENT '批次模式：ORDERS_ONLY=仅生成订单，FULL_DATA=生成用户、渠道关系及订单，LEGACY=升级前历史模拟数据',
 MODIFY COLUMN retain_data BOOLEAN NOT NULL DEFAULT FALSE COMMENT '本批次是否保留：0=停止或启动恢复时清理，1=保留本批次生成数据',
 MODIFY COLUMN started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '批次开始时间，毫秒精度，业务时区为Asia/Shanghai',
 MODIFY COLUMN ended_at DATETIME(3) NULL DEFAULT NULL COMMENT '批次结束时间，毫秒精度，业务时区为Asia/Shanghai；NULL表示尚未记录结束（运行中或异常退出未恢复）';
