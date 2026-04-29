package com.zzp.pvms.media.controller;

import com.zzp.pvms.common.api.Result;
import com.zzp.pvms.media.domain.Tag;
import com.zzp.pvms.media.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public Result<List<Tag>> listAll() {
        return Result.success(tagService.list());
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody Tag tag) {
        return Result.success(tagService.save(tag));
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

    @GetMapping("/media/{mediaId}")
    public Result<List<Tag>> getByMedia(@PathVariable Long mediaId) {
        return Result.success(tagService.getTagsByMediaId(mediaId));
    }
}
