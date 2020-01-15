package com.alibaba.rsocket.metadata;

import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.metadata.WellKnownMimeType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RSocket Message accept mimetypes metadata
 *
 * @author leijuan
 */
public class MessageAcceptMimeTypesMetadata implements MetadataAware {
    private List<Object> mimeTypes = new ArrayList<>();
    private int byteBufLength = 0;

    public MessageAcceptMimeTypesMetadata() {
    }

    public MessageAcceptMimeTypesMetadata(String... acceptedMimeTypes) {
        for (String acceptedMimeType : acceptedMimeTypes) {
            WellKnownMimeType wellKnownMimeType = WellKnownMimeType.fromString(acceptedMimeType);
            if (wellKnownMimeType == null) {
                this.mimeTypes.add(acceptedMimeType);
                this.byteBufLength += (acceptedMimeTypes.length + 1);
            } else {
                this.mimeTypes.add(wellKnownMimeType.getIdentifier());
                this.byteBufLength += 1;
            }
        }
    }

    public MessageAcceptMimeTypesMetadata(WellKnownMimeType... wellKnownMimeTypes) {
        for (WellKnownMimeType wellKnownMimeType : wellKnownMimeTypes) {
            this.mimeTypes.add(wellKnownMimeType.getIdentifier());
        }
        this.byteBufLength = wellKnownMimeTypes.length;
    }

    public MessageAcceptMimeTypesMetadata(RSocketMimeType... rsocketMimeTypes) {
        for (RSocketMimeType rsocketMimeType : rsocketMimeTypes) {
            this.mimeTypes.add(rsocketMimeType.getId());
        }
        this.byteBufLength = rsocketMimeTypes.length;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.MessageAcceptMimeTypes;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.MessageAcceptMimeTypes.getType();
    }

    public RSocketMimeType getFirstAcceptType() {
        Object mimeType = mimeTypes.get(0);
        if (mimeType instanceof Byte) {
            return RSocketMimeType.valueOf((Byte) mimeType);
        } else if (mimeType instanceof String) {
            return RSocketMimeType.valueOfType((String) mimeType);
        }
        return null;
    }

    public ByteBuf getContent() {
        ByteBuf buffer = Unpooled.buffer(this.byteBufLength);
        for (Object mimeType : mimeTypes) {
            if (mimeType instanceof Byte) {
                buffer.writeByte((byte) ((byte) mimeType | 0x80));
            } else if (mimeType instanceof String) {
                byte[] bytes = ((String) mimeType).getBytes();
                buffer.writeByte(bytes.length);
                buffer.writeBytes(bytes);
            }
        }
        return buffer;
    }

    @Override
    public void load(ByteBuf byteBuf) {
        this.byteBufLength = byteBuf.capacity();
        while (byteBuf.isReadable()) {
            byte firstByte = byteBuf.readByte();
            if (firstByte < 0) {
                byte mimeTypeId = (byte) (firstByte & 0x7F);
                this.mimeTypes.add(WellKnownMimeType.fromIdentifier(mimeTypeId).getString());
            } else {
                byteBuf.readCharSequence(firstByte, StandardCharsets.US_ASCII);
            }
        }
    }

    @Override
    public String toText() throws Exception {
        return Joiner.on(',').join(mimeTypes);
    }

    @Override
    public void load(String text) throws Exception {
        String[] parts = text.split(",");
        for (String part : parts) {
            if (part.contains("/")) {
                this.mimeTypes.add(part);
            } else {
                this.mimeTypes.add(Byte.valueOf(part));
            }
        }
    }
}
