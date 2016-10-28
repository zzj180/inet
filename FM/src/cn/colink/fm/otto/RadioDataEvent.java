package cn.colink.fm.otto;

public class RadioDataEvent {
    public final byte[] mPacket;
    public RadioDataEvent(byte[] packet) {
        this.mPacket = packet;
    }
}
