package renderer;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.*;
import model.points.Meeple;
import model.state.State;
import sge.CarcassonneAction;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentRenderer {

    private static final int TILE_SIZE = 80;
    private State state;
    private double zoom = 1.0;
    private double offsetX = 700;
    private double offsetY = 450;

    private final Map<Integer, BufferedImage> tileImages = new HashMap<>();

    private final JPanel panel = new JPanel() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (state == null) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(offsetX, offsetY);
            g.scale(zoom, zoom);

            state.getTiles().iterator().forEachRemaining(mapEntry->{
                long t = PositionTileMapLayoutBit.getValue(mapEntry);
                int x = TileLayoutBit.getX(t) * TILE_SIZE;
                int y = -TileLayoutBit.getY(t) * TILE_SIZE;
                int rot = TileLayoutBit.getRotation(t);
                int tileId = TileLayoutBit.getTileId(t);
                var startTile = state.getGameConfig().startTile();
                var tileSpec = tileId == startTile.getTileId() ? startTile : state.getGameConfig().tiles()[tileId];
                int graphicsId = tileSpec.getGraphicsId();
                BufferedImage image = tileImages.computeIfAbsent(graphicsId, AgentRenderer::loadTileImage);
                AffineTransform old = g.getTransform();
                g.translate(x + TILE_SIZE / 2.0, y + TILE_SIZE / 2.0);
                g.rotate(Math.toRadians(-rot * 90.0));
                g.drawImage(image, -TILE_SIZE / 2, -TILE_SIZE / 2, TILE_SIZE, TILE_SIZE, null);
                g.setTransform(old);
            });
            var meeples = state.getMeepleRegistry().getMeeples();
            var areas = state.getAreaRegistry();
            for(int i = 0; i < meeples.length;i++){
                int meeple = meeples[i];
                if(!Meeple.isFree(meeple)){
                    long area = areas.areas[MeepleLayoutBit.getGlobalAreaId(meeple)];
                    state.getTiles().iterator().forEachRemaining(entry->{
                        long t = PositionTileMapLayoutBit.getValue(entry);
                        if(TileLayoutBit.getTileId(t) == AreaLayoutBit.getTileId(area)){
                            Point meepleP = getMeeplePoint(AreaLayoutBit.getLocalAreaId(area));
                            int globalMeepleId = MeepleLayoutBit.getGlobalMeepleId(meeple);
                            int meeplesPerPlayer = state.getGameConfig().getMeeplePerPlayer();
                            int playerId = Meeple.getPlayerIdFromGlobal(globalMeepleId, meeplesPerPlayer);
                            g.setColor(playerId == 0 ? Color.BLUE : Color.RED);
                            int x = TileLayoutBit.getX(t) * TILE_SIZE;
                            int y = -TileLayoutBit.getY(t) * TILE_SIZE;
                            int rot = TileLayoutBit.getRotation(t);
                            AffineTransform old = g.getTransform();
                            g.translate(x + TILE_SIZE / 2.0, y + TILE_SIZE / 2.0);
                            g.rotate(Math.toRadians(-rot * 90.0));
                            g.fillOval(meepleP.x - TILE_SIZE / 2 - 8, meepleP.y - TILE_SIZE / 2 - 8, 16, 16);
                            g.setTransform(old);
                        }
                    });
                }
            }
            g.scale(1 / zoom, 1 / zoom);
            g.translate(-offsetX, -offsetY);
            g.setColor(Color.BLACK);
        }
    };


    public void initRender() {
        panel.setPreferredSize(new Dimension(1400, 900));
        panel.setBackground(Color.WHITE);

        MouseAdapter mouse = new MouseAdapter() {
            private Point last;

            @Override
            public void mousePressed(MouseEvent event) {
                last = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                offsetX += event.getX() - last.x;
                offsetY += event.getY() - last.y;
                last = event.getPoint();
                panel.repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom *= event.getPreciseWheelRotation() < 0 ? 1.1 : 0.9;
                zoom = Math.max(0.2, Math.min(5.0, zoom));
                panel.repaint();
            }
        };

        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);
        panel.addMouseWheelListener(mouse);

        JFrame frame = new JFrame("AgentRenderer");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void setBoard(State state) {
        this.state = state;
        panel.repaint();
    }

    private static BufferedImage loadTileImage(int graphicsId) {
        String resource = String.format("/tiles/%02d.png", graphicsId);
        try (InputStream stream = AgentRenderer.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException exception) {
            return null;
        }
    }

    private static Point getMeeplePoint(int areaId) {
        int edge = TILE_SIZE / 6;
        int corner = TILE_SIZE / 4;
        int center = TILE_SIZE / 2;
        int farCorner = TILE_SIZE - corner;
        int farEdge = TILE_SIZE - edge;

        return switch (areaId) {
            case 0 -> new Point(edge, farCorner);
            case 1 -> new Point(edge, center);
            case 2 -> new Point(edge, corner);
            case 3 -> new Point(corner, edge);
            case 4 -> new Point(center, edge);
            case 5 -> new Point(farCorner, edge);
            case 6 -> new Point(farEdge, corner);
            case 7 -> new Point(farEdge, center);
            case 8 -> new Point(farEdge, farCorner);
            case 9 -> new Point(farCorner, farEdge);
            case 10 -> new Point(center, farEdge);
            case 11 -> new Point(corner, farEdge);
            case CarcassonneAction.MONASTERY_AREA_ID -> new Point(center, center);
            default -> new Point(center, center);
        };
    }
}
