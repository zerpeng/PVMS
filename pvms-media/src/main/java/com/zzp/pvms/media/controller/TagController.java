package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.AuthContext;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.domain.dto.IdsRequest;
import com.zzp.pvms.media.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public Result<List<Tag>> listAll(HttpServletRequest request) {
        return Result.success(tagService.listActiveTags(auth(request)));
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag, HttpServletRequest request) {
        return Result.success(tagService.createTag(tag, auth(request)));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag, HttpServletRequest request) {
        return Result.success(tagService.updateTag(id, tag, auth(request)));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(tagService.removeTags(List.of(id), auth(request)));
    }

    @DeleteMapping
    public Result<Boolean> removeBatch(@RequestBody(required = false) IdsRequest idsRequest, HttpServletRequest request) {
        return Result.success(tagService.removeTags(idsRequest == null ? null : idsRequest.getIds(), auth(request)));
    }

    @PostMapping("/bind")
    public Result<String> bind(@RequestParam Long mediaId, @RequestParam Long tagId, HttpServletRequest request) {
        tagService.addTagToMedia(mediaId, tagId, auth(request));
        return Result.success("绑定成功");
    }

    @PostMapping("/unbind")
    public Result<String> unbind(@RequestParam Long mediaId, @RequestParam Long tagId, HttpServletRequest request) {
        tagService.removeTagFromMedia(mediaId, tagId, auth(request));
        return Result.success("解绑成功");
    }

    @DeleteMapping("/{tagId}/media/{mediaId}")
    public Result<String> removeMedia(@PathVariable Long tagId, @PathVariable Long mediaId, HttpServletRequest request) {
        tagService.removeTagFromMedia(mediaId, tagId, auth(request));
        return Result.success("移除成功");
    }

    @GetMapping("/media/{mediaId}")
    public Result<List<Tag>> getByMedia(@PathVariable Long mediaId, HttpServletRequest request) {
        return Result.success(tagService.getTagsByMediaId(mediaId, auth(request)));
    }

    private AuthContext auth(HttpServletRequest request) {
        return AuthContext.of(
                request.getHeader("X-User-Name"),
                request.getHeader("X-User-Role"),
                request.getHeader("X-Super-Admin"),
                request.getHeader("X-Visitor"),
                request.getHeader("X-Directory-Ids"),
                request.getHeader("X-Album-Ids"),
                request.getHeader("X-Tag-Ids"));
    }
}
