/**
 * SA-MP App - Query and RCON Application
 *
 * @author Rafael 'R@f' Keramidas <rafael@keramid.as>
 * @version 2.0.5
 * @date 8th May 2012
 * @licence GPLv3
 * @thanks StatusRed : Took example of this query class code for the v0.2.0.
 * Sasuke78200 : Some help with the first query class (v0.1.x).
 * Woothemes.com : In app icons (tabs and menu).
 * TheOriginalTwig : App icon.
 */

package com.wander.imagepalette;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class SampQuery {
    private InetAddress serverip = null;
    private int serverport = 0;
    private String serverrcon = null;
    private boolean serverstatus = false;
    private DatagramSocket socket = null;

    public SampQuery(String srvip, int srvport, String srvrcon) {
        try {
            this.serverip = InetAddress.getByName(srvip);
            this.serverport = srvport;
            this.serverrcon = srvrcon;

            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1000);

            if (this.sendPacket('p', "")) {
                String result = new String(this.receiveData());
                result = result.substring(10).trim();

                if (result.equals("p1234")) {
                    this.serverstatus = true;
                } else {
                    this.serverstatus = false;
                }
            } else {
                this.serverstatus = false;
            }
        } catch (Exception e) {
            this.serverstatus = false;
        }
    }

    public void socketClose() {
        socket.close();
    }

    public boolean isOnline() {
        return this.serverstatus;
    }

    public String[] getInfos() {
        String[] infos = new String[6];

        try {
            if (this.sendPacket('i', "")) {
                byte[] result = this.receiveData();
                ByteBuffer buff = ByteBuffer.wrap(result);
                buff.order(ByteOrder.LITTLE_ENDIAN);
                buff.position(11);

                /* Password */
                if (buff.get() == 0)
                    infos[0] = "No";
                else
                    infos[0] = "Yes";

                /* Players */
                infos[1] = String.valueOf(buff.getShort());

                /* Max Players */
                infos[2] = String.valueOf(buff.getShort());

                /* Hostname */
                int hostnamelenght = buff.getInt();
                infos[3] = convertGBK(buff,hostnamelenght);

                /* Gamemode */
                int gamemodelenght = buff.getInt();
                infos[4] = convertGBK(buff,gamemodelenght);

                /* Map name */
                int mapnamelenght = buff.getInt();
                infos[5] = convertGBK(buff,mapnamelenght);

                return infos;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String[][] getRules() {
        try {
            if (this.sendPacket('r', "")) {
                byte[] result = this.receiveData();
                ByteBuffer buff = ByteBuffer.wrap(result);
                buff.order(ByteOrder.LITTLE_ENDIAN);
                buff.position(11);

                int rulescount = buff.getShort();
                String[][] rules = new String[2][rulescount];
                for (int i = 0; i < rulescount; i++) {
                    int rulenamelength = buff.get();
                    rules[0][i] = convertGBK(buff,rulenamelength);

                    int rulevaluelength = buff.get();
                    rules[1][i] =convertGBK(buff,rulevaluelength);
                }

                return rules;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String[][] getPlayers() {
        try {
            if (this.sendPacket('d', "")) {
                byte[] result = this.receiveData();
                ByteBuffer buff = ByteBuffer.wrap(result);
                buff.order(ByteOrder.LITTLE_ENDIAN);
                buff.position(11);

                int playercount = buff.getShort();
                String[][] players = new String[4][playercount];

                for (int i = 0; i < playercount; i++) {
                    /* ID */
                    int playerid = (int) buff.get() & 0xff;
                    players[0][i] = String.valueOf(playerid);

                    /* Player name */
                    int playernamelenght = buff.get();
                    players[1][i] = convertGBK(buff, playernamelenght);

                    /* Score */
                    players[2][i] = String.valueOf(buff.getInt());

                    /* Ping */
                    players[3][i] = String.valueOf(buff.getInt());

                }

                return players;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String convertGBK(ByteBuffer buff, int length) throws UnsupportedEncodingException {
        byte[] n = new byte[length];
        for (int x = 0; x < length; x++)
            n[x] = buff.get();

        return new String(n,"gbk");
    }

    public boolean sendRconCommand(String command) {
        try {
            if (this.sendPacket('x', command)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidRconPassword() {
        try {
            if (this.sendPacket('x', "")) {
                String result = new String(this.receiveData());
                result = result.substring(13).trim();
                if (result.equals("Invalid RCON password.")) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendPacket(char type, String command) {
        DatagramPacket pkt = null;
        String pktdata = "";
        byte[] IP = this.serverip.getAddress();

        try {
            pktdata = "SAMP";
            for (int i = 0; i < 4; i++)
                pktdata += (char) IP[i];
            pktdata += (char) (this.serverport & 0xFF);
            pktdata += (char) ((this.serverport >> 8) & 0xFF);
            /* PING */
            if (type == 'p') {
                pktdata += "p";

                pktdata += "1";
                pktdata += "2";
                pktdata += "3";
                pktdata += "4";
            }
            /* INFO */
            else if (type == 'i') {
                pktdata += "i";
            }
            /* RULES */
            else if (type == 'r') {
                pktdata += "r";
            }
            /* PLAYERS */
            else if (type == 'd') {
                pktdata += "d";
            }
            /* RCON */
            else if (type == 'x') {
                pktdata += "x";

                pktdata += (char) (this.serverrcon.length() & 0xFF);
                pktdata += (char) (this.serverrcon.length() >> 8 & 0xFF);
                pktdata += this.serverrcon;
                pktdata += (char) (command.length() & 0xFF);
                pktdata += (char) (command.length() >> 8 & 0xFF);
                pktdata += command;
            } else {
                pktdata += "p";
            }

            byte[] data = pktdata.getBytes("US-ASCII");
            pkt = new DatagramPacket(data, data.length, this.serverip, this.serverport);
            this.socket.send(pkt);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] receiveData() {
        byte[] data = new byte[3072];
        DatagramPacket getpacket = null;

        try {
            getpacket = new DatagramPacket(data, data.length);
            socket.receive(getpacket);
        } catch (Exception e) {
        }

        return getpacket.getData();
    }
}
