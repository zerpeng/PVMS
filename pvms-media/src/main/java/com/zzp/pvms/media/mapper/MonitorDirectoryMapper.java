package com.zzp.pvms.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzp.pvms.media.domain.MonitorDirectory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MonitorDirectoryMapper extends BaseMapper<MonitorDirectory> {
    @Select("""
            SELECT id, dir_path, is_recursive, scan_status, last_scan_time, del_flag, create_time
            FROM monitor_directory
            WHERE dir_path = #{path}
            LIMIT 1
            """)
    MonitorDirectory selectAnyByPath(@Param("path") String path);
}
