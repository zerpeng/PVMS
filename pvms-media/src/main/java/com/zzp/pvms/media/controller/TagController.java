package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.domain.dto.IdsRequest;
import com.zzp.pvms.media.service.TagService;
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
    public Result<List<Tag>> listAll() {
        return Result.success(tagService.listActiveTags());
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        return Result.success(tagService.createTag(tag));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        return Result.success(tagService.updateTag(id, tag));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(tagService.removeTags(List.of(id)));
    }

    @DeleteMapping
    public Result<Boolean> removeBatch(@RequestBody(required = false) IdsRequest request) {
        return Result.success(tagService.removeTags(request == null ? null : request.getIds()));
    }

    @PostMapping("/bind")
    public Result<String> bind(@RequestParam Long mediaId, @RequestParam Long tagId) {
        tagService.addTagToMedia(mediaId, tagId);
        return Result.success("绑定成功");
    }

    @PostMapping("/unbind")
    public Result<String> unbind(@RequestParam Long mediaId, @RequestParam Long tagId) {
        tagService.removeTagFromMedia(mediaId, tagId);
        return Result.success("解绑成功");
    }

    @DeleteMapping("/{tagId}/media/{mediaId}")
    public Result<String> removeMedia(@PathVariable Long tagId, @PathVariable Long mediaId) {
        tagService.removeTagFromMedia(mediaId, tagId);
        return Result.success("移除成功！！！");
    }

    @GetMapping("/media/{mediaId}")
    public Result<List<Tag>> getByMedia(@PathVariable Long mediaId) {
        return Result.success(tagService.getTagsByMediaId(mediaId));
    }
}
