package org.icpclive.backend.player.widgets;

import org.icpclive.backend.Preparation;
import org.icpclive.backend.graphics.AbstractGraphics;
import org.icpclive.backend.player.widgets.stylesheets.QueueStylesheet;
import org.icpclive.events.ContestInfo;
import org.icpclive.backend.player.widgets.stylesheets.PlateStyle;
import org.icpclive.datapassing.CachedData;
import org.icpclive.datapassing.Data;
import org.icpclive.events.ProblemInfo;
import org.icpclive.events.TeamInfo;
import org.icpclive.events.RunInfo;

import java.awt.*;
import java.util.*;
import java.util.List;

public class QueueWidget extends Widget {

    private static final double V = 0.005;

    private static final long WAIT_TIME = 60000;
    private static final long FIRST_TO_SOLVE_WAIT_TIME = 120000;
    private static final int MAX_QUEUE_SIZE = 16;

    static double Y_SHIFT;

    private final int baseX;
    private final int baseY;
    private final int plateHeight;
    private final int spaceY;
    private final int spaceX;

    private final int nameWidth;
    private final int rankWidth;
    private final int solvedWidth;
    private final int problemWidth;
    private final int statusWidth;

    private final int videoHeight;

    ContestInfo info;
    private boolean showVerdict;

    private NewBreakingNewsWidget breakingNews;
    private RunPlate breaking;

    private class RunPlate {
        double currentPosition;
        double desiredPosition;
        double visibilityState;
        RunInfo runInfo;
        boolean visible;

        RunPlate(RunInfo runInfo) {
            this.runInfo = runInfo;
        }
    }

    private Map<Integer, RunPlate> plates = new HashMap<>();

    public QueueWidget(int baseX, int baseY, int plateHeight, long updateWait, boolean showVerdict) {
        super(updateWait);

        this.baseX = baseX;
        this.baseY = baseY;
        this.plateHeight = plateHeight;

        spaceX = (int) Math.round(plateHeight * SPACE_X);
        spaceY = (int) Math.round(plateHeight * SPACE_Y);

        nameWidth = (int) Math.round(NAME_WIDTH * plateHeight);
        rankWidth = (int) Math.round(RANK_WIDTH * plateHeight);
        solvedWidth = (int) Math.round(PROBLEM_WIDTH * plateHeight);
        problemWidth = (int) Math.round(PROBLEM_WIDTH * plateHeight);
        statusWidth = (int) Math.round(STATUS_WIDTH * plateHeight);

        setFont(Font.decode(MAIN_FONT + " " + (int) (plateHeight * 0.7)));

        this.showVerdict = showVerdict;

        int videoWidth = problemWidth + solvedWidth + nameWidth + rankWidth + statusWidth;
        videoHeight = videoWidth * 9 / 16;

        breakingNews = new NewBreakingNewsWidget(updateWait, videoWidth, videoHeight);

        setVisibilityState(1);
        setVisible(true);
    }

    @Override
    public void paint(AbstractGraphics g, int width, int height, double scale) {
        super.paint(g, width, height, scale);
        breakingNews.paint(g, width, height, scale);
    }

    @Override
    public void paintImpl(AbstractGraphics g, int width, int height) {
        super.paintImpl(g, width, height);
        move(width, height, updateVisibilityState());
        graphics.clip(baseX - width, baseY - height, 2 * width, height);
        List<RunPlate> list = new ArrayList<>(plates.values());
        Collections.sort(list, (o1, o2) -> -Double.compare(o1.desiredPosition, o2.desiredPosition));
        for (RunPlate plate : list) {
            drawRun(baseX, baseY + (int) (plate.currentPosition * (plateHeight + spaceY)), plate);
        }
        if (breaking != null) {
            breakingNews.setPosition(baseX, baseY + (int) (breaking.currentPosition * (plateHeight + spaceY)) - videoHeight);
        }
    }


    @Override
    protected void move(int width, int height, int dt) {
        calculateQueue();
        if (info == null) return;

        for (RunInfo r : info.getRuns()) {
            if (r == null)
                continue;
            RunPlate plate = getRunPlate(r);

            double dp = dt * V;
            if (Math.abs(plate.currentPosition - plate.desiredPosition) < dp) {
                plate.currentPosition = plate.desiredPosition;
            } else {
                if (plate.desiredPosition < plate.currentPosition) {
                    plate.currentPosition -= dp;
                } else {
                    plate.currentPosition += dp;
                }

            }
            if (plate.visible) {
                plate.visibilityState = Math.min(plate.visibilityState + dt * 0.001, 1);
            } else {
                plate.visibilityState = Math.max(plate.visibilityState - dt * 0.001, 0);
            }
            if (plate.visibilityState == 0) {
                plates.remove(plate.runInfo.getId());
            }
        }

    }

    private RunPlate getRunPlate(RunInfo r) {
        RunPlate plate = plates.get(r.getId());
        if (plate == null) {
            plate = new RunPlate(r);
            plates.put(r.getId(), plate);
        }
        return plate;
    }

    private void drawRun(int x, int y, RunPlate plate) {

        boolean blinking = breakingNews.isVisible() && plate == breaking;

        setVisibilityState(plate.visibilityState);

        RunInfo runInfo = plate.runInfo;
        TeamInfo team = info.getParticipant(runInfo.getTeamId());
        String name = team.getShortName();
        ProblemInfo problem = info.problems.get(runInfo.getProblemNumber());
        String result = runInfo.getResult();

        PlateStyle teamColor = QueueStylesheet.name;
        PlateStyle resultColor = QueueStylesheet.udProblem;

        boolean inProgress = false;
        int progressWidth = 0;

        if (runInfo.isJudged()) {
            if (runInfo.isAccepted()) {
                resultColor = QueueStylesheet.acProblem;
            } else {
                resultColor = QueueStylesheet.waProblem;
            }
        } else {
            inProgress = true;
            progressWidth = (int) Math.round(statusWidth * runInfo.getPercentage());
        }

//        if (plate.desiredPosition > 0) {
//            return;
//        }

//        double v = odd ? 1 : 0.9;

        PlateStyle color = getTeamRankColor(team);
        applyStyle(color);
        drawRectangleWithText("" + Math.max(team.getRank(), 1), x, y,
                rankWidth, plateHeight, PlateStyle.Alignment.CENTER, blinking);

        x += rankWidth + spaceX;

        applyStyle(teamColor);
        drawRectangleWithText(name, x, y,
                nameWidth, plateHeight, PlateStyle.Alignment.LEFT, blinking);

        x += nameWidth + spaceX;

        drawRectangleWithText("" + team.getSolvedProblemsNumber(), x, y,
                solvedWidth, plateHeight, PlateStyle.Alignment.CENTER, blinking);

        x += solvedWidth;

        drawProblemPane(problem, x, y, problemWidth, plateHeight, blinking);

        if (showVerdict) {
            x += problemWidth + spaceX;

            if (runInfo.getTime() > ContestInfo.FREEZE_TIME) {
                result = "?";
                resultColor = QueueStylesheet.frozenProblem;
                inProgress = false;
            }

            applyStyle(resultColor);
            drawRectangleWithText(result, x, y, statusWidth,
                    plateHeight, PlateStyle.Alignment.CENTER, blinking);

            if (inProgress) {
                graphics.drawRect(x, y, progressWidth, plateHeight, QueueStylesheet.udTests,
                        visibilityState * plate.visibilityState, PlateStyle.RectangleType.SOLID);
            }
            if (plate.runInfo == info.firstSolvedRun()[runInfo.getProblemNumber()]) {
                drawStar(x + statusWidth - STAR_SIZE, y + 2 * STAR_SIZE, STAR_SIZE);
            }
        } else {
            if (plate.runInfo == info.firstSolvedRun()[runInfo.getProblemNumber()]) {
                drawStar(x + problemWidth - STAR_SIZE, y + 2 * STAR_SIZE, STAR_SIZE);
            }
        }

    }

    private void calculateQueue() {
        info = Preparation.eventsLoader.getContestData();

        for (RunPlate plate : plates.values()) {
            plate.visible = false;
            plate.desiredPosition = 0;
        }

        RunPlate breaking = null;
        if (breakingNews.visibilityState > 0.01) {
            RunInfo run = breakingNews.getRun();
            breaking = this.breaking = getRunPlate(run);
        }
        List<RunPlate> firstToSolves = new ArrayList<>();
        List<RunPlate> queue = new ArrayList<>();

        for (RunInfo r : info.getRuns()) {
            if (r == null)
                continue;
            if (breaking != null && breaking.runInfo == r) {
                continue;
            }

            if (r == info.firstSolvedRun()[r.getProblemNumber()]) {
                if (r.getLastUpdateTime() > info.getCurrentTime() - FIRST_TO_SOLVE_WAIT_TIME) {
                    firstToSolves.add(getRunPlate(r));
                }
            } else {
//                System.out.println(r.getLastUpdateTime() + " " + info.getCurrentTime());
                if (r.getLastUpdateTime() > info.getCurrentTime() - WAIT_TIME) {
                    queue.add(getRunPlate(r));
                }
            }
        }

        int extra = firstToSolves.size() + queue.size() - MAX_QUEUE_SIZE;
        if (extra > 0) {
            queue.clear();

            for (RunInfo r : info.getRuns()) {
                if (r == null)
                    continue;
                if (r.getTime() >= info.getCurrentTime()) {
                    continue;
                }
                if (breaking != null && breaking.runInfo == r) {
                    continue;
                }
                if (r == info.firstSolvedRun()[r.getProblemNumber()]) {
                    continue;
                }
                if (r.getLastUpdateTime() > info.getCurrentTime() - WAIT_TIME) {
                    if ((r.isJudged() || r.getTime() > ContestInfo.FREEZE_TIME) && extra > 0) {
                        extra--;
                        continue;
                    }
                    queue.add(getRunPlate(r));
                }
            }
        }

        for (RunPlate plate : plates.values()) {
            plate.visible = false;
            plate.desiredPosition = 0;
        }
        double pos = -queue.size();
        for (RunPlate plate : queue) {
            plate.desiredPosition = pos - Y_SHIFT;
            plate.visible = true;
            pos += 1;
        }

        pos = -queue.size() - firstToSolves.size();
        if (queue.size() > 0) pos -= 0.5;
        for (RunPlate plate : firstToSolves) {
            plate.desiredPosition = pos - Y_SHIFT;
            plate.visible = true;
            pos += 1;
        }

        pos -= firstToSolves.size() + 1;
        if (firstToSolves.size() > 0) pos -= 0.5;
        if (breaking != null) {
            breaking.desiredPosition = pos - Y_SHIFT;
            breaking.visible = true;
        }

        Y_SHIFT = 0;
    }

    @Override
    protected void update() {
        super.update();
        breakingNews.update();
    }

    @Override
    protected void updateImpl(Data data) {
        setVisible(data.queueData.isQueueVisible());
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    protected CachedData getCorrespondingData(Data data) {
        return data.queueData;
    }

}