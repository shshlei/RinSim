/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.core.Simulator;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEvent;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.AbstractRoadModel.RoadEvent;
import rinde.sim.core.model.road.MoveEvent;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class FabriRechtProblem extends ScenarioController {

	protected final FabriRechtScenario fabriRechtScenario;
	protected final StatisticsListener statisticsListener;

	/**
	 * @param scen
	 * @param numberOfTicks
	 */
	public FabriRechtProblem(FabriRechtScenario scen) {
		super(scen, (int) (scen.timeWindow.end - scen.timeWindow.begin));
		fabriRechtScenario = scen;
		statisticsListener = new StatisticsListener();
	}

	// subclasses can override this method to add more models
	@Override
	protected Simulator createSimulator() throws Exception {
		final Simulator sim = new Simulator(new MersenneTwister(123), 1);
		final RoadModel rm = new PlaneRoadModel(fabriRechtScenario.min, fabriRechtScenario.max, false, 1.0);
		final PDPModel pm = new PDPModel(rm, new TardyAllowedPolicy());
		sim.getEventAPI().addListener(statisticsListener, SimulatorEventType.values());
		rm.getEventAPI().addListener(statisticsListener, RoadEvent.MOVE);
		pm.getEventAPI()
				.addListener(statisticsListener, PDPModelEventType.START_PICKUP, PDPModelEventType.END_PICKUP, PDPModelEventType.START_DELIVERY, PDPModelEventType.END_DELIVERY);
		sim.register(rm);
		sim.register(pm);
		return sim;
	}

	@Override
	protected final boolean handleTimedEvent(TimedEvent event) {
		if (event.getEventType() == PDPScenarioEvent.ADD_PARCEL) {
			statisticsListener.addedParcels++;
			return handleAddParcel(((AddParcelEvent) event));
		} else if (event.getEventType() == PDPScenarioEvent.ADD_VEHICLE) {
			return handleAddVehicle((AddVehicleEvent) event);
		} else if (event.getEventType() == PDPScenarioEvent.ADD_DEPOT) {
			return handleAddDepot((AddDepotEvent) event);
		} else if (event.getEventType() == PDPScenarioEvent.TIME_OUT) {
			getSimulator().stop();
			return handleTimeOut();
		}
		return false;
	}

	protected abstract boolean handleAddVehicle(AddVehicleEvent event);

	protected abstract boolean handleTimeOut();

	protected boolean handleAddParcel(AddParcelEvent event) {
		return getSimulator().register(new FRParcel(event.parcelDTO));
	}

	protected boolean handleAddDepot(AddDepotEvent event) {
		return getSimulator().register(new FRDepot(event.position));
	}

	public StatisticsDTO getStatistics() {
		return statisticsListener.getDTO();
	}

	public class StatisticsDTO {
		public final double totalDistance;
		public final int totalPickups;
		public final int totalDeliveries;
		public final int addedParcels;
		public final long pickupTardiness;
		public final long deliveryTardiness;
		public final long computationTime;
		public final long simulationTime;

		public StatisticsDTO(double dist, int pick, int del, int parc, long pickTar, long delTar, long compT, long simT) {
			totalDistance = dist;
			totalPickups = pick;
			totalDeliveries = del;
			addedParcels = parc;
			pickupTardiness = pickTar;
			deliveryTardiness = delTar;
			computationTime = compT;
			simulationTime = simT;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("\t\t\t = Statistics = \ncomputation time:\t\t").append(computationTime);
			sb.append("\nsimulation time:\t\t").append(simulationTime);
			sb.append("\ntotal traveled distance:\t").append(totalDistance);
			sb.append("\npickups:\t\t\t").append(totalPickups).append(" / ").append(addedParcels);
			sb.append("\t").append(Math.round(((double) totalPickups / addedParcels) * 100d)).append("%");
			sb.append("\ndeliveries:\t\t\t").append(totalDeliveries).append(" / ").append(addedParcels);
			sb.append("\t").append(Math.round(((double) totalDeliveries / addedParcels) * 100d)).append("%");
			sb.append("\npickup tardiness:\t\t").append(pickupTardiness);
			sb.append("\ndelivery tardiness:\t\t").append(deliveryTardiness);
			return sb.toString();
		}
	}

	public class StatisticsListener implements Listener {

		protected final Map<MovingRoadUser, Double> distanceMap;
		protected double totalDistance;
		protected int totalPickups;
		protected int totalDeliveries;
		protected long pickupTardiness;
		protected long deliveryTardiness;
		protected int addedParcels;
		protected long startTimeReal;
		protected long startTimeSim;
		protected long computationTime;
		protected long simulationTime;

		public StatisticsListener() {
			distanceMap = newLinkedHashMap();
			totalDistance = 0d;
			totalPickups = 0;
			totalDeliveries = 0;
			addedParcels = 0;
		}

		@Override
		public void handleEvent(Event e) {
			if (e.getEventType() == SimulatorEventType.STARTED) {
				startTimeReal = System.currentTimeMillis();
				startTimeSim = getSimulator().getCurrentTime();
			} else if (e.getEventType() == SimulatorEventType.STOPPED) {
				computationTime = System.currentTimeMillis() - startTimeReal;
				simulationTime = getSimulator().getCurrentTime() - startTimeSim;
			} else if (e.getEventType() == RoadEvent.MOVE) {
				final MoveEvent me = ((MoveEvent) e);
				increment(me.roadUser, me.pathProgress.distance);
				totalDistance += me.pathProgress.distance;
			} else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getPickupTimeWindow().begin - pme.parcel.getPickupDuration();
				if (pme.time > latestBeginTime) {
					pickupTardiness += pme.time - latestBeginTime;
				}

			} else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
				totalPickups++;
			} else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getDeliveryTimeWindow().begin
						- pme.parcel.getDeliveryDuration();
				if (pme.time > latestBeginTime) {
					deliveryTardiness += pme.time - latestBeginTime;
				}
			} else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
				totalDeliveries++;
			}
		}

		protected void increment(MovingRoadUser mru, double num) {
			if (!distanceMap.containsKey(mru)) {
				distanceMap.put(mru, num);
			} else {
				distanceMap.put(mru, distanceMap.get(mru) + num);
			}
		}

		public double getTotalTraveledDistance() {
			return totalDistance;
		}

		public int getTotalPickups() {
			return totalPickups;
		}

		public int getTotalDeliveries() {
			return totalDeliveries;
		}

		public long getComputationTime() {
			return computationTime;
		}

		public StatisticsDTO getDTO() {
			return new StatisticsDTO(totalDistance, totalPickups, totalDeliveries, addedParcels, pickupTardiness,
					deliveryTardiness, computationTime, simulationTime);
		}
	}

}
