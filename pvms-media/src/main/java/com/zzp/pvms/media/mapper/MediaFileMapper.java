package com.zzp.pvms.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.media.domain.MediaFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface MediaFileMapper extends BaseMapper<MediaFile> {

    @Select("""
            <script>
            SELECT mf.*,
                   CASE WHEN fav.id IS NULL THEN 0 ELSE 1 END AS favorited
            FROM media_file mf
            INNER JOIN monitor_directory md ON md.id = mf.directory_id
            LEFT JOIN media_favorite fav
              ON fav.media_id = mf.id
             AND fav.username = #{username}
            WHERE mf.del_flag = 0
              AND md.del_flag = 0
              <if test="!superAdmin and albumId == null and tagId == null">
                <choose>
                  <when test="directoryIds != null and directoryIds.size() > 0">
                    AND mf.directory_id IN
                    <foreach collection="directoryIds" item="directoryId" open="(" separator="," close=")">
                      #{directoryId}
                    </foreach>
                  </when>
                  <otherwise>
                    AND 1 = 0
                  </otherwise>
                </choose>
              </if>
              <if test="mediaType != null">
                AND mf.media_type = #{mediaType}
              </if>
              <if test="albumId != null">
                AND EXISTS (
                  SELECT 1
                  FROM media_album_relation mar
                  WHERE mar.media_id = mf.id
                    AND mar.album_id = #{albumId}
                )
              </if>
              <if test="albumId == null and !superAdmin and albumIds != null and albumIds.size() > 0">
                AND (
                  NOT EXISTS (SELECT 1 FROM media_album_relation mar_scope WHERE mar_scope.media_id = mf.id)
                  OR EXISTS (
                    SELECT 1
                    FROM media_album_relation mar_scope
                    WHERE mar_scope.media_id = mf.id
                      AND mar_scope.album_id IN
                      <foreach collection="albumIds" item="scopeAlbumId" open="(" separator="," close=")">
                        #{scopeAlbumId}
                      </foreach>
                  )
                )
              </if>
              <if test="tagId != null">
                AND EXISTS (
                  SELECT 1
                  FROM media_tag_relation mtr
                  WHERE mtr.media_id = mf.id
                    AND mtr.tag_id = #{tagId}
                )
              </if>
              <if test="favoriteOnly != null and favoriteOnly">
                AND fav.id IS NOT NULL
              </if>
            ORDER BY mf.shot_time DESC, mf.create_time DESC
            </script>
            """)
    IPage<MediaFile> selectActivePage(
            Page<MediaFile> page,
            @Param("mediaType") Integer mediaType,
            @Param("albumId") Long albumId,
            @Param("tagId") Long tagId,
            @Param("favoriteOnly") Boolean favoriteOnly,
            @Param("username") String username,
            @Param("superAdmin") boolean superAdmin,
            @Param("directoryIds") List<Long> directoryIds,
            @Param("albumIds") List<Long> albumIds
    );

    @Select("""
            SELECT *
            FROM media_file
            WHERE file_path = #{filePath}
            LIMIT 1
            """)
    MediaFile selectAnyByFilePath(@Param("filePath") String filePath);

    @Update("""
            UPDATE media_file
            SET file_name = #{fileName},
                file_hash = #{fileHash},
                file_size = #{fileSize},
                media_type = #{mediaType},
                file_format = #{fileFormat},
                shot_time = #{shotTime},
                width = #{width},
                height = #{height},
                duration = #{duration},
                camera_make = #{cameraMake},
                camera_model = #{cameraModel},
                lens_model = #{lensModel},
                f_number = #{fNumber},
                exposure_time = #{exposureTime},
                iso = #{iso},
                focal_length = #{focalLength},
                gps_latitude = #{gpsLatitude},
                gps_longitude = #{gpsLongitude},
                location_name = #{locationName},
                thumbnail_path = #{thumbnailPath},
                directory_id = #{directoryId},
                del_flag = 0,
                update_time = NOW()
            WHERE id = #{id}
            """)
    int restoreById(MediaFile mediaFile);

    @Select("""
            SELECT mf.*
            FROM media_file mf
            INNER JOIN monitor_directory md ON md.id = mf.directory_id
            WHERE mf.id = #{id}
              AND mf.del_flag = 0
              AND md.del_flag = 0
            LIMIT 1
            """)
    MediaFile selectActiveById(@Param("id") Long id);

    @Select("""
            SELECT mf.shot_time AS shotTime
            FROM media_file mf
            INNER JOIN monitor_directory md ON md.id = mf.directory_id
            WHERE mf.del_flag = 0
              AND md.del_flag = 0
            GROUP BY mf.shot_time
            ORDER BY mf.shot_time DESC
            """)
    List<Map<String, Object>> selectActiveTimeline();
}
