package ru.ifmo.acm.events.WF;

import ru.ifmo.acm.events.ContestInfo;
import ru.ifmo.acm.events.RunInfo;
import ru.ifmo.acm.events.TeamInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by aksenov on 05.05.2015.
 */
public class WFContestInfo extends ContestInfo {
	private WFRunInfo[] runs;
	String[] languages;
	private WFTeamInfo[] teamInfos;
	public long[] timeFirstSolved;

	private WFTeamInfo[] standings = null;

	public WFContestInfo(int problemsNumber, int teamsNumber) {
        problemNumber = problemsNumber;
        teamNumber = teamsNumber;
		teamInfos = new WFTeamInfo[teamsNumber];
		timeFirstSolved = new long[problemsNumber];
		languages = new String[4];
		runs = new WFRunInfo[1000000];
	}

	void recalcStandings() {
		WFTeamInfo[] standings = new WFTeamInfo[teamNumber];
		int n = 0;
		Arrays.fill(timeFirstSolved, Integer.MAX_VALUE);
		for (WFTeamInfo team : teamInfos) {
			if (team == null)
				continue;

			team.solved = 0;
			team.penalty = 0;
			team.lastAccepted = 0;
			for (int j = 0; j < problemNumber; j++) {
				List<RunInfo> runs = team.getRuns()[j];
				int wrong = 0;
				for (RunInfo run : runs) {
					WFRunInfo wfrun = (WFRunInfo) run;
					if ("AC".equals(run.getResult())) {
						team.solved++;
						int time = (int) (wfrun.getTime() / 60 / 1000);
						team.penalty += wrong * 20 + time;
						team.lastAccepted = Math.max(team.lastAccepted, time);
						timeFirstSolved[j] = Math.min(timeFirstSolved[j], wfrun.getTime());
						break;
					} else if (wfrun.getResult().length() > 0) {
						wrong++;
					}
				}
			}
			standings[n++] = team;
		}

		Arrays.sort(standings, 0, n, TeamInfo.comparator);

		for (int i = 0; i < n; i++) {
			if (i > 0 && TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
				standings[i].rank = standings[i - 1].rank;
			} else {
				standings[i].rank = i + 1;
			}
		}
		this.standings = standings;
	}

	public void addTeam(WFTeamInfo team) {
		teamInfos[team.getId()] = team;
	}

	public boolean runExists(int id) {
		return runs[id] != null;
	}

	public WFRunInfo getRun(int id) {
		return runs[id];
	}

	public void addRun(WFRunInfo run) {
		if (!runExists(run.getId())) {
			runs[run.getId()] = run;
			teamInfos[run.getTeam()].addRun(run, run.getProblemNumber());
		}
	}

	public void addTest(WFTestCaseInfo test) {
		System.out.println("Adding test " + test.id + " to run " + test.run);
		if (runExists(test.run)) {
			runs[test.run].add(test);
			System.out.println("Run " + runs[test.run] + " passed " + runs[test.run].getPassedTestsNumber() + " tests");
		}
	}

	@Override
	public TeamInfo getParticipant(String name) {
		for (int i = 0; i < teamNumber; i++) {
			if (teamInfos[i + 1].getName().equals(name) || teamInfos[i + 1].getShortName().equals(name)) {
				return teamInfos[i + 1];
			}
		}
		return null;
	}

	@Override
	public TeamInfo getParticipant(int id) {
		return teamInfos[id];
	}

	public TeamInfo[] getStandings() {
		return standings;
	}

	@Override
	public long[] firstTimeSolved() {
		return timeFirstSolved;
	}

	@Override
	public RunInfo[] getRuns() {
		return runs;
	}
}
