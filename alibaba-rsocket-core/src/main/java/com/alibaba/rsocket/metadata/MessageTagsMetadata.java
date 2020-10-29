package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.metadata.TaggingMetadata;
import io.rsocket.metadata.TaggingMetadataCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * message tags metadata
 *
 * @author leijuan
 */
public class MessageTagsMetadata implements MetadataAware {
    /**
     * credentials,
     */
    private Map<String, String> tags;

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public MessageTagsMetadata() {
        this.tags = new HashMap<>();
    }

    public MessageTagsMetadata(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.MessageTags;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.MessageTags.getType();
    }

    @Override
    public ByteBuf getContent() {
        List<String> temp = new ArrayList<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            temp.add(entry.getKey() + "=" + entry.getValue());
        }
        return TaggingMetadataCodec.createTaggingContent(ByteBufAllocator.DEFAULT, temp);
    }

    /**
     * format routing as "r:%s,s:%s" style
     *
     * @return data format
     */
    private String formatData() {
        return this.tags.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return formatData();
    }


    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) {
        TaggingMetadata taggingMetadata = new TaggingMetadata(RSocketMimeType.MessageTags.getType(), byteBuf);
        taggingMetadata.forEach(pair -> {
            int start = pair.indexOf("=");
            String name = pair.substring(0, start);
            String value = pair.substring(start + 1);
            tags.put(name, value);
        });
    }

    public static MessageTagsMetadata from(ByteBuf content) {
        MessageTagsMetadata temp = new MessageTagsMetadata();
        temp.load(content);
        return temp;
    }
}
