package com.zzp.pvms.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.media.domain.MediaFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            @Param("username") String username
    );

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
