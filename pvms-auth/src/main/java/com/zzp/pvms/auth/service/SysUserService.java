package com.zzp.pvms.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzp.pvms.auth.domain.dto.UserRequest;
import com.zzp.pvms.auth.domain.vo.UserImportResult;
import com.zzp.pvms.auth.domain.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SysUserService {
    Page<UserVO> queryPage(int current, int size, String username, String nickname, String roleKey, Integer status);

    UserVO create(UserRequest request);

    UserVO update(Long id, UserRequest request);

    boolean remove(Long id, String currentUsername);

    byte[] exportCsv(String username, String nickname, String roleKey, Integer status);

    UserImportResult importCsv(MultipartFile file) throws IOException;
}
