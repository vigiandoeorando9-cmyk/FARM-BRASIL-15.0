package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ALL_VEHICLES
import com.example.domain.FISH_SPECIES_CATALOG
import com.example.domain.GameSettings
import com.example.domain.ManualTool
import com.example.domain.OwnedVehicle
import com.example.domain.PlotZoneType
import com.example.domain.PondSizeTier
import com.example.domain.SoilState
import com.example.domain.VehicleCategory
import com.example.domain.getEarthCapacityM3
import com.example.domain.getVehicleDef
import com.example.domain.getWaterTankCapacityL
import com.example.domain.isEarthmovingVehicle
import com.example.domain.isWaterTankerVehicle
import com.example.simulation.ControlInput
import com.example.simulation.DiagnosticEngine
import com.example.simulation.WATER_PROBE_PRICE_BRL
import com.example.simulation.advanceDailyAquaculture
import com.example.simulation.advanceDay
import com.example.simulation.buildCommercialWaterReservoirInWorld
import com.example.simulation.buildOrConvertExcavationToPondInWorld
import com.example.simulation.buyAndFeedPondFish
import com.example.simulation.buyVehicleInWorld
import com.example.simulation.buyWaterProspectingToolInWorld
import com.example.simulation.createNewWorld
import com.example.simulation.dumpEarthFromMachineInWorld
import com.example.simulation.enablePiscicultureOnPond
import com.example.simulation.ensureWaterAndFishWorldInitialized
import com.example.simulation.evaluateProspectingAt
import com.example.simulation.executeEarthworkActionInWorld
import com.example.simulation.fillWaterTankerInWorld
import com.example.simulation.harvestPondFish
import com.example.simulation.installPondPumpAndPipingInWorld
import com.example.simulation.maintainPondWaterQuality
import com.example.simulation.markWaterProspectPointInWorld
import com.example.simulation.performDrillingRigSoundingInWorld
import com.example.simulation.pumpWaterFromPondToReservoir
import com.example.simulation.sellHarvestedFishAtMarket
import com.example.simulation.sellWaterCommercialDeliveryInWorld
import com.example.simulation.stepSimulation
import com.example.simulation.stockFishFingerlingsInPond
import com.example.simulation.upgradePondDikesInWorld
import com.example.state.PerfSample
import com.example.storage.CareerImportResult
import com.example.storage.exportCareerToJson
import com.example.storage.loadWorldFromSlot
import com.example.storage.parseAndVerifyCareerImport
import com.example.storage.saveWorldToSlot
import com.example.ui.screens.PerformanceMonitorScreen
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AquacultureWaterEarthworksUpdateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalog_includesAllEarthmovingDrillingAndWaterTankerCategories() {
        val cats = ALL_VEHICLES.map { it.category }.toSet()
        assertThat(cats).containsAtLeast(
            VehicleCategory.ESCAVADEIRA,
            VehicleCategory.RETROESCAVADEIRA,
            VehicleCategory.MOTONIVELADORA,
            VehicleCategory.TRATOR_ESTEIRA,
            VehicleCategory.CARREGADEIRA,
            VehicleCategory.CAMINHAO,
            VehicleCategory.PERFURATRIZ,
            VehicleCategory.CAMINHAO_PIPA,
        )

        val earthmovers = ALL_VEHICLES.filter { isEarthmovingVehicle(it) }
        val tankers = ALL_VEHICLES.filter { isWaterTankerVehicle(it) }
        assertThat(earthmovers.size).isAtLeast(6)
        assertThat(tankers.size).isAtLeast(2)
        earthmovers.filter { it.category != VehicleCategory.PERFURATRIZ }.forEach {
            assertThat(getEarthCapacityM3(it)).isGreaterThan(0.5)
        }
        tankers.forEach {
            assertThat(getWaterTankCapacityL(it)).isAtLeast(12000.0)
        }
    }

    @Test
    fun groundwaterProspecting_isDeterministicAndMarksMapPointsAndDrillingRigSounding() {
        val base = ensureWaterAndFishWorldInitialized(
            createNewWorld(0, "Fazenda Aquífero", "map_pr", 2_000_000.0, "Padrão", 64)
        )
        assertThat(base.aquiferGrid.orEmpty()).isNotEmpty()

        val probeWorld = buyWaterProspectingToolInWorld(base)
        assertThat(probeWorld.hasWaterProbeTool).isTrue()
        assertThat(probeWorld.player.tool).isEqualTo(ManualTool.SONDA_AGUA)
        assertThat(probeWorld.moneyBrl).isWithin(0.01).of(2_000_000.0 - WATER_PROBE_PRICE_BRL)

        val aq = probeWorld.aquiferGrid!!.maxByOrNull { it.qualityPct }!!
        val scan1 = evaluateProspectingAt(probeWorld, aq.centerX, aq.centerZ)
        val scan2 = evaluateProspectingAt(probeWorld, aq.centerX, aq.centerZ)
        assertThat(scan1.availability).isEqualTo(scan2.availability)
        assertThat(scan1.estimatedDepthM).isWithin(0.01).of(scan2.estimatedDepthM)
        assertThat(scan1.audioBeepIntervalMs).isIn(200..960)

        val moved = probeWorld.copy(player = probeWorld.player.copy(x = aq.centerX, z = aq.centerZ))
        val marked = markWaterProspectPointInWorld(moved, method = "SONDA_MANUAL")
        assertThat(marked.waterProspectPoints.orEmpty()).hasSize(1)
        assertThat(marked.waterProspectPoints!!.first().availability).isEqualTo(scan1.availability)

        // Drilling rig sounding
        val withRig = buyVehicleInWorld(marked, "sonda_geo_pd120")
        val rig = withRig.vehicles.last()
        val withActiveRig = withRig.copy(
            activeVehicleUid = rig.uid,
            vehicles = withRig.vehicles.map { if (it.uid == rig.uid) it.copy(x = 12.0, z = 12.0, engineRunning = true) else it },
        )
        val sounded = performDrillingRigSoundingInWorld(withActiveRig)
        assertThat(sounded.waterProspectPoints.orEmpty().size).isEqualTo(2)
        assertThat(sounded.earthworkMarks.orEmpty().any { it.kind == "DRILL_BORE" }).isTrue()
    }

    @Test
    fun excavationGradingDumping_modifiesTerrainGeometryAndConvertsToDiscountedPond() {
        val base = ensureWaterAndFishWorldInitialized(
            createNewWorld(0, "Fazenda Terraplanagem", "map_go", 3_500_000.0, "Padrão", 64)
        )
        val ownedPlot = base.plots.first { it.owned && !it.isPreservation && it.zoneType == PlotZoneType.RURAL_CULTIVO }
        val digX = ownedPlot.centerX ?: 0.0
        val digZ = ownedPlot.centerZ ?: 0.0

        val withExcavator = buyVehicleInWorld(base, "cat_320d_esc")
        val exc = withExcavator.vehicles.last()
        val readyWorld = withExcavator.copy(
            activeVehicleUid = exc.uid,
            vehicles = withExcavator.vehicles.map {
                if (it.uid == exc.uid) it.copy(x = digX, z = digZ, headingDeg = 0.0, engineRunning = true) else it
            },
        )

        var excavated = executeEarthworkActionInWorld(readyWorld)
        val modifiedCells = excavated.cells.filter { (it.excavationDepthM ?: 0.0) > 0.4 }
        assertThat(modifiedCells.size).isAtLeast(2)
        assertThat(excavated.earthworkMarks.orEmpty()).isNotEmpty()

        val activeExc = excavated.vehicles.first { it.uid == exc.uid }
        assertThat(activeExc.carriedEarthM3 ?: 0.0).isGreaterThan(0.5)

        // Move and dump dirt to create spoil mound / berm
        val beforeDumpSpoilCount = excavated.cells.count { (it.spoilHeightM ?: 0.0) > 0.05 }
        val dumped = dumpEarthFromMachineInWorld(
            excavated.copy(
                vehicles = excavated.vehicles.map {
                    if (it.uid == exc.uid) it.copy(x = digX + 18.0, z = digZ) else it
                }
            )
        )
        val afterDumpSpoilCount = dumped.cells.count { (it.spoilHeightM ?: 0.0) > 0.05 }
        assertThat(afterDumpSpoilCount).isGreaterThan(beforeDumpSpoilCount)

        // Second bucket pass deepens multiple basin cells (>= 0.45m) before conversion
        val secondPass = executeEarthworkActionInWorld(
            dumped.copy(
                vehicles = dumped.vehicles.map {
                    if (it.uid == exc.uid) it.copy(x = digX + 2.0, z = digZ) else it
                }
            )
        )

        // Convert excavated depression into a Pond at 25% of normal cost
        val moneyBeforeConvert = secondPass.moneyBrl
        val withPond = buildOrConvertExcavationToPondInWorld(
            state = secondPass,
            pondName = "Açude Escavado Sul",
            tier = PondSizeTier.PEQUENO,
            customX = digX,
            customZ = digZ,
            fromExistingExcavation = true,
        )
        assertThat(withPond.ponds.orEmpty().any { it.name == "Açude Escavado Sul" }).isTrue()
        assertThat(moneyBeforeConvert - withPond.moneyBrl).isWithin(1.0).of(PondSizeTier.PEQUENO.buildCostBrl * 0.25)
    }

    @Test
    fun pondFiniteAquiferRecharge_commercialWaterSales_andTankerTruckFlow() {
        val base = ensureWaterAndFishWorldInitialized(
            createNewWorld(0, "Fazenda Hídrica", "map_pr", 4_000_000.0, "Padrão", 64)
        )
        val ownedPlot = base.plots.first { it.owned && !it.isPreservation }
        val px = ownedPlot.centerX ?: 0.0
        val pz = ownedPlot.centerZ ?: 0.0

        val withPond = buildOrConvertExcavationToPondInWorld(
            state = base,
            pondName = "Açude Rio Claro",
            tier = PondSizeTier.MEDIO,
            customX = px,
            customZ = pz,
            fromExistingExcavation = false,
        )
        val pond = withPond.ponds.orEmpty().last()
        val dikeUp = upgradePondDikesInWorld(withPond, pond.uid)
        assertThat(dikeUp.ponds!!.last().dikeIntegrityPct).isGreaterThan(pond.dikeIntegrityPct)

        val pumped = installPondPumpAndPipingInWorld(dikeUp, pond.uid)
        assertThat(pumped.ponds!!.last().hasPump).isTrue()
        assertThat(pumped.ponds!!.last().pumpActive).isTrue()

        // Step simulation forward so pump draws from finite linked aquifer
        val aqBefore = pumped.aquiferGrid!!.find { it.id == pond.linkedAquiferId }!!.remainingCapacityL
        var sim = pumped
        repeat(80) {
            sim = stepSimulation(sim, ControlInput(), 0.25)
        }
        val aqAfter = sim.aquiferGrid!!.find { it.id == pond.linkedAquiferId }!!.remainingCapacityL
        val pondAfter = sim.ponds!!.last()
        assertThat(pondAfter.waterVolumeL).isGreaterThan(pond.waterVolumeL)
        assertThat(aqAfter).isLessThan(aqBefore) // Finite aquifer depletes!

        // Build commercial reservoir, pump pond water, and sell via network + tanker truck
        val withRes = buildCommercialWaterReservoirInWorld(sim)
        assertThat(withRes.waterBusiness?.hasReservoirStructure).isTrue()
        val filledRes = pumpWaterFromPondToReservoir(withRes, pond.uid, 15_000.0)
        assertThat(filledRes.waterBusiness!!.reservoirVolumeL).isGreaterThan(5_000.0)

        val soldDirect = sellWaterCommercialDeliveryInWorld(filledRes, 5_000.0)
        assertThat(soldDirect.waterBusiness!!.totalWaterSoldL).isGreaterThan(4_900.0)
        assertThat(soldDirect.waterBusiness!!.totalWaterRevenueBrl).isGreaterThan(500.0)

        val withPipa = buyVehicleInWorld(soldDirect, "vw_constellation_pipa_12k")
        val pipa = withPipa.vehicles.last()
        val nearPondPipa = withPipa.copy(
            activeVehicleUid = pipa.uid,
            vehicles = withPipa.vehicles.map { if (it.uid == pipa.uid) it.copy(x = px + 4.0, z = pz) else it },
        )
        val filledPipa = fillWaterTankerInWorld(nearPondPipa)
        assertThat(filledPipa.vehicles.first { it.uid == pipa.uid }.carriedWaterL ?: 0.0).isGreaterThan(5000.0)
        val soldPipa = sellWaterCommercialDeliveryInWorld(filledPipa, null)
        assertThat(soldPipa.waterBusiness!!.totalWaterRevenueBrl).isGreaterThan(soldDirect.waterBusiness!!.totalWaterRevenueBrl)

        // Advance day triggers natural aquifer recharge (capped at totalCapacityL)
        val nextDayState = advanceDay(soldPipa)
        val aqNextDay = nextDayState.aquiferGrid!!.find { it.id == pond.linkedAquiferId }!!.remainingCapacityL
        assertThat(aqNextDay).isAtMost(nextDayState.aquiferGrid!!.find { it.id == pond.linkedAquiferId }!!.totalCapacityL)
    }

    @Test
    fun piscicultureLifecycle_stockFeedMaintainGrowHarvestAndSellFish() {
        val base = ensureWaterAndFishWorldInitialized(
            createNewWorld(0, "Fazenda Peixe", "map_pr", 2_500_000.0, "Padrão", 64)
        )
        val readyPlot = base.plots.first { !it.isPreservation && it.owned }
        val withPond = buildOrConvertExcavationToPondInWorld(
            state = base,
            pondName = "Lago Piscícola",
            tier = PondSizeTier.MEDIO,
            customX = readyPlot.centerX,
            customZ = readyPlot.centerZ,
            fromExistingExcavation = false,
        )
        val pondUid = withPond.ponds!!.last().uid
        // Ensure adequate water level for stocking
        val watered = withPond.copy(
            ponds = withPond.ponds!!.map {
                if (it.uid == pondUid) it.copy(waterLevelM = 2.4, waterVolumeL = 320_000.0) else it
            }
        )
        val enabled = enablePiscicultureOnPond(watered, pondUid)
        assertThat(enabled.ponds!!.last().piscicultureEnabled).isTrue()

        val stocked = stockFishFingerlingsInPond(enabled, pondUid, FISH_SPECIES_CATALOG.first().id, 200)
        assertThat(stocked.ponds!!.last().fishStocks).hasSize(1)

        val fed = buyAndFeedPondFish(stocked, pondUid, 120.0)
        assertThat(fed.ponds!!.last().fishFeedStockKg).isAtLeast(120.0)

        var grown = maintainPondWaterQuality(fed, pondUid, "AERACAO")
        repeat(10) {
            grown = advanceDailyAquaculture(grown)
            grown = maintainPondWaterQuality(grown, pondUid, "AERACAO")
        }
        val finalStock = grown.ponds!!.last().fishStocks.first()
        assertThat(finalStock.growthProgress).isAtLeast(0.95)
        assertThat(finalStock.readyToHarvest).isTrue()

        val harvested = harvestPondFish(grown, pondUid, finalStock.speciesId)
        assertThat(harvested.inventoryKg["peixe_${finalStock.speciesId}"] ?: 0.0).isGreaterThan(50.0)

        val sold = sellHarvestedFishAtMarket(harvested, finalStock.speciesId)
        assertThat(sold.moneyBrl).isGreaterThan(harvested.moneyBrl)
        assertThat(sold.waterBusiness?.totalFishRevenueBrl ?: 0.0).isGreaterThan(500.0)
    }

    @Test
    fun persistenceRoundtripAnd2000StepStressAudit_preserveStateWithZeroErrors() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val base = ensureWaterAndFishWorldInitialized(
            createNewWorld(7, "Fazenda Persistência", "map_mt", 3_000_000.0, "Padrão", 64)
        )
        val withProbe = markWaterProspectPointInWorld(buyWaterProspectingToolInWorld(base))
        val withPond = buildOrConvertExcavationToPondInWorld(
            state = withProbe,
            pondName = "Açude Matriz",
            tier = PondSizeTier.GRANDE,
            customX = withProbe.plots.first { it.owned }.centerX,
            customZ = withProbe.plots.first { it.owned }.centerZ,
            fromExistingExcavation = false,
        )

        var stressed = withPond
        repeat(2000) { idx ->
            val input = if (idx % 120 < 60) ControlInput(throttle = 0.65, moveX = 0.15) else ControlInput()
            stressed = stepSimulation(stressed, input, 0.05)
        }
        val audit = DiagnosticEngine.runWorldIntegrityAudit(stressed)
        assertThat(audit.errorCount).isEqualTo(0)
        assertThat(audit.activePondsCount).isAtLeast(1)

        // Save/load to CareerStorage + JSON export/import roundtrip
        val (savedOk, _) = saveWorldToSlot(ctx, stressed)
        assertThat(savedOk).isTrue()
        val (loaded, recovered) = loadWorldFromSlot(ctx, stressed.slotIndex)
        assertThat(recovered).isFalse()
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.ponds.orEmpty().size).isEqualTo(stressed.ponds.orEmpty().size)
        assertThat(loaded.aquiferGrid.orEmpty().size).isEqualTo(stressed.aquiferGrid.orEmpty().size)
        assertThat(loaded.waterProspectPoints.orEmpty().size).isEqualTo(stressed.waterProspectPoints.orEmpty().size)
        assertThat(loaded.hasWaterProbeTool).isTrue()

        val exportedJson = exportCareerToJson(stressed)
        val imported = parseAndVerifyCareerImport(exportedJson, 8)
        assertThat(imported).isInstanceOf(CareerImportResult.Ok::class.java)
    }

    @Test
    fun performanceMonitorScreen_rendersDiagnosticsAndTriggersAudit() {
        var audited = false
        composeRule.setContent {
            FarmBrasilTheme {
                PerformanceMonitorScreen(
                    perfStats = PerfSample(fps = 60, frameTimeMs = 15.0, activeCells = 4096, vehiclesCount = 3, implementsCount = 4),
                    settings = GameSettings(),
                    onApplyPreset = {},
                    onToggleAutoOptimize = {},
                    onBack = {},
                    diagnosticSnapshot = DiagnosticEngine.snapshotFlow.value,
                    onRunSelfDiagnostics = { audited = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("perf_monitor_list")
            .performScrollToNode(hasTestTag("run_diagnostics_audit_btn"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("run_diagnostics_audit_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(audited).isTrue()
    }
}
