# 示例商品图

使用内置 imagegen 工具生成，未调用 CLI 或外部商品平台。图片为虚构服装，仅用于本地后台演示。

最终文件：`src/main/resources/static/images/catalog-01.jpg` 至 `catalog-24.jpg`。原始联系表按商品单元拆分为 24 张独立 JPEG；拆分不改变服装内容。

排列：每款两种配色，每个配色包含正面、背面两图。6 款依次为棉质 T 恤、牛津纺衬衫、西装裤、飞行夹克、撞色 T 恤、牛仔裤。

生成提示词：

> Use case: product-mockup. Create a precisely aligned clothing product catalog contact sheet, 4 columns by 6 rows, 24 equal cells, 2:3 portrait aspect. Every cell identical pale cool gray seamless studio background, consistent soft light, isolated garment flat lay centered with generous margin, no humans, no text, no logos, no separators, no borders. Each row a different garment with four views: first color front, first color back, second color front, second color back. Row 1: heavy cotton crewneck short sleeve tshirt, ivory then charcoal. Row 2: relaxed long sleeve Oxford button-down shirt, pale blue then white. Row 3: straight leg tailored trousers, black then stone taupe. Row 4: zip front lightweight bomber jacket, olive green then navy. Row 5: short sleeve raglan tshirt, cream body with red sleeves then cream body with black sleeves. Row 6: wide leg denim jeans, indigo then pale washed blue. Exactly four cells per row and six rows; garments entirely contained inside each cell. Professional realistic catalog photography with fabric detail.

