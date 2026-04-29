package com.zzp.pvms.processor.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.zzp.pvms.common.dto.MediaProcessMsg;
import com.zzp.pvms.processor.domain.MediaFile;
import com.zzp.pvms.processor.mapper.MediaFileMapper;
import com.zzp.pvms.processor.service.ThumbnailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ThumbnailServiceImpl implements ThumbnailService {

    @Value("${pvms.processor.thumbnail-path}")
    private String baseThumbnailPath;

    @Value("${pvms.processor.thumbnail-width:320}")
    private int thumbnailWidth;

    @Value("${pvms.processor.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Override
    public void createThumbnail(MediaProcessMsg msg) {
        String sourcePath = msg.getFilePath();
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            log.error("源文件不存在: {}", sourcePath);
            return;
        }

        FileUtil.mkdir(baseThumbnailPath);
        String thumbnailName = IdUtil.simpleUUID() + ".jpg";
        File targetFile = new File(baseThumbnailPath, thumbnailName);

        try {
            boolean success = false;
            if (isVideo(sourcePath)) {
                // 视频优先使用系统命令行 FFmpeg 处理，以更好地支持中文路径
                success = generateVideoThumbnailCmd(sourceFile, targetFile);
            } else {
                success = generateImageThumbnail(sourceFile, targetFile);
            }

            if (success && targetFile.exists() && targetFile.length() > 0) {
                log.info("缩略图生成成功: {} -> {}", sourcePath, targetFile.getAbsolutePath());
                MediaFile mediaFile = new MediaFile();
                mediaFile.setId(msg.getMediaId());
                mediaFile.setThumbnailPath(targetFile.getAbsolutePath());
                mediaFileMapper.updateById(mediaFile);
            } else {
                log.error("缩略图生成失败，文件未生成或为空: {}", sourcePath);
            }
        } catch (Exception e) {
            log.error("处理缩略图异常: {}", sourcePath, e);
        }
    }

    /**
     * 图片缩略图：使用 Hutool
     */
    private boolean generateImageThumbnail(File source, File target) {
        try {
            ImgUtil.scale(source, target, (float) thumbnailWidth / getWidth(source));
            return true;
        } catch (Exception e) {
            log.warn("图片解析失败: {}", source.getName());
            return false;
        }
    }

    /**
     * 视频缩略图：直接调用系统 FFmpeg 命令行
     * 这种方式对 Windows 中文路径支持最好
     */
    private boolean generateVideoThumbnailCmd(File source, File target) {
        try {
            // 构建命令: ffmpeg -ss 00:00:01 -i "input" -vf scale=320:-1 -vframes 1 "output.jpg" -y
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-ss");
            command.add("00:00:01");
            command.add("-i");
            command.add(source.getAbsolutePath());
            command.add("-vf");
            command.add("scale=" + thumbnailWidth + ":-1");
            command.add("-vframes");
            command.add("1");
            command.add("-y"); // 覆盖现有文件
            command.add(target.getAbsolutePath());

            log.debug("执行 FFmpeg 命令: {}", String.join(" ", command));
            
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true); // 合并错误流到标准输出
            Process process = builder.start();
            
            // 等待处理完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 进程退出异常，退出码: {}", exitCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("调用系统 FFmpeg 异常", e);
            return false;
        }
    }

    private int getWidth(File file) {
        try {
            BufferedImage read = ImageIO.read(file);
            return read == null ? 1024 : read.getWidth();
        } catch (Exception e) {
            return 1024;
        }
    }

    private boolean isVideo(String path) {
        String ext = FileUtil.extName(path).toLowerCase();
        return "mp4".equals(ext) || "mov".equals(ext) || "avi".equals(ext) || "mkv".equals(ext) || "flv".equals(ext);
    }
}
