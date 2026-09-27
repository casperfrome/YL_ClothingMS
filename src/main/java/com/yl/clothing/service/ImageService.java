package com.yl.clothing.service;

import com.yl.clothing.common.Checks;
import com.yl.clothing.converter.ProductConverter;
import com.yl.clothing.entity.ProductImage;
import com.yl.clothing.exception.BusinessException;
import com.yl.clothing.mapper.ProductMapper;
import com.yl.clothing.vo.ImageVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private final ProductMapper mapper;
    private final ProductService products;
    private final Path root;

    public ImageService(ProductMapper mapper, ProductService products, @Value("${app.upload-dir}") String directory) {
        this.mapper = mapper;
        this.products = products;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ImageVO> upload(long colorId, List<MultipartFile> files) throws IOException {
        products.color(colorId);
        mapper.lockColor(colorId);
        if (files.isEmpty()) {
            throw new BusinessException(400, "请选择图片");
        }
        List<ProductImage> existing = mapper.images(colorId);
        if (existing.size() + files.size() > 100) {
            throw new BusinessException(400, "每个配色最多保存 100 张图片");
        }
        Files.createDirectories(root);
        int order = existing.stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
        List<Path> created = new ArrayList<>();
        // Database and filesystem changes share a rollback boundary, including checked IOExceptions.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    created.forEach(ImageService::remove);
                }
            }
        });
        for (MultipartFile file : files) {
            String format = validatedFormat(file);
            String name = UUID.randomUUID() + "." + (format.equals("jpeg") ? "jpg" : format);
            Path target = root.resolve(name);
            created.add(target);
            file.transferTo(target);
            ProductImage image = new ProductImage();
            image.setColorId(colorId);
            image.setUrl("/uploads/" + name);
            image.setSortOrder(order++);
            mapper.insertImage(image);
        }
        return mapper.images(colorId).stream().map(ProductConverter::image).toList();
    }

    private String validatedFormat(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(400, "图片不能为空且不能超过 5MB");
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BusinessException(400, "文件不是有效的 JPEG、PNG 或 WebP 图片");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg", "jpg", "png", "webp").contains(format)) {
                    throw new BusinessException(400, "仅支持 JPEG、PNG、WebP");
                }
                reader.setInput(stream);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 25_000_000) {
                    throw new BusinessException(400, "图片像素总量不能超过 2500 万");
                }
                if (reader.read(0) == null) {
                    throw new BusinessException(400, "无法读取图片");
                }
                return format;
            } finally {
                reader.dispose();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(400, "图片文件损坏或格式不支持");
        }
    }

    @Transactional
    public void delete(long id) {
        ProductImage image = Checks.require(mapper.image(id), "图片");
        products.color(image.getColorId());
        mapper.lockColor(image.getColorId());
        mapper.deleteImage(id);
        String url = image.getUrl();
        if (url.startsWith("/uploads/")) {
            Path target = root.resolve(url.substring(9)).normalize();
            if (target.getParent().equals(root)) {
                // Remove persisted files only after the database deletion commits.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        remove(target);
                    }
                });
            }
        }
    }

    @Transactional
    public List<ImageVO> sort(long colorId, List<Long> ids) {
        products.color(colorId);
        mapper.lockColor(colorId);
        Set<Long> actual = new HashSet<>(mapper.images(colorId).stream().map(ProductImage::getId).toList());
        if (ids.size() != actual.size() || new HashSet<>(ids).size() != ids.size() || !actual.equals(new HashSet<>(ids))) {
            throw new BusinessException(400, "请按顺序提交该配色的全部图片，且不能重复");
        }
        for (int i = 0; i < ids.size(); i++) {
            mapper.sortImage(ids.get(i), i);
        }
        return mapper.images(colorId).stream().map(ProductConverter::image).toList();
    }

    private static void remove(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("无法移除图片 {}", path, exception);
        }
    }
}
