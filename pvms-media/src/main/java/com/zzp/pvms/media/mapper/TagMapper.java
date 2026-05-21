package com.zzp.pvms.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzp.pvms.media.domain.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    @Select("""
            SELECT id, parent_id, name, tag_type, category, create_username, del_flag
            FROM tag
            WHERE name = #{name}
              AND tag_type = #{tagType}
            LIMIT 1
            """)
    Tag selectAnyByNameAndType(@Param("name") String name, @Param("tagType") Integer tagType);

    @Update("""
            UPDATE tag
            SET parent_id = #{parentId},
                category = #{category},
                create_username = #{createUsername},
                del_flag = 0
            WHERE id = #{id}
            """)
    int restoreById(Tag tag);
}
