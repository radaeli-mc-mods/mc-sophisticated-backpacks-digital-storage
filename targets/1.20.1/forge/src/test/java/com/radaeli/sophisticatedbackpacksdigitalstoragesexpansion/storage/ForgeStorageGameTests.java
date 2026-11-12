package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.PumpWorldTargetsTest;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePumpUpgradeWrapperTest;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.PrioritySettingsTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.gametest.GameTestHolder;

/** Runs the ported NeoForge storage matrix inside the transformed Forge server runtime. */
@GameTestHolder(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgeStorageGameTests {
    private ForgeStorageGameTests() {}

    @GameTest(template = "empty")
    public static void depositFilter(GameTestHelper helper) {
        DepositFilterTest tests = new DepositFilterTest();
        tests.allowAndBlockModesUseNativeSophisticatedFilters();
        tests.inventoryModeMatchesDestinationContentsInsteadOfTheManualList();
        tests.nativeNbtMatchingDistinguishesNamedItems();
        tests.destinationSnapshotsAreDefensiveAndReadOnly();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void endpointBinding(GameTestHelper helper) {
        EndpointBindingTest tests = new EndpointBindingTest();
        tests.keepsExistingBindingFormatAndPreservesUnrelatedData();
        tests.replacingALinkDoesNotLeaveThePreviousEndpoint();
        tests.unknownBackendsRemainReadableWithoutLoadingTheirMod();
        tests.incompleteOrMalformedBindingsAreIgnored();
        tests.endpointsDoNotRetainMutablePositions();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guardedFluidHandler(GameTestHelper helper) {
        GuardedFluidHandlerTest tests = new GuardedFluidHandlerTest();
        tests.cachedCapabilityIsBlockedDuringTransferAndResumesAfterwards();
        tests.cachedViewTracksTheCurrentBackpackContentsAndScopeReleasesOnException();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemTransfers(GameTestHelper helper) {
        ItemTransferServiceTest tests = new ItemTransferServiceTest();
        tests.movesOnlyMatchingStacksAndReportsActualCounts();
        tests.restoresDifferenceBetweenSimulatedAndActualAcceptance();
        tests.zeroActualAcceptanceRestoresEverythingAndReportsNoMovement();
        for (int simulated : new int[]{0, -1, Integer.MIN_VALUE}) tests.nonPositiveSimulationNeverExtracts(simulated);
        tests.simulationCannotOfferMoreThanTheSourceContains();
        tests.extractionCanReturnLessThanRequested();
        tests.aSourceThatRefusesExtractionDoesNotInsert();
        tests.restoresIntoAnotherSourceSlotBeforeUsingRecovery();
        tests.recoveryReceivesEveryItemThatNoLongerFitsInSource();
        tests.componentsSurvivePartialInsertionAndRecovery();
        tests.backendInputMutationDoesNotMutateTheSourceOrAccounting();
        tests.nativeLongAcceptedCountsAreClampedBeforeConversionToInt();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpTransfers(GameTestHelper helper) {
        PumpTransferServiceTest tests = new PumpTransferServiceTest();
        tests.realNeoForgeBucketCapabilitiesWorkInBothDirectionsWithNoBackpackTank();
        tests.splitExtractionPersistsFirstSourceBeforeCallingTheSecond();
        tests.inputSplitsOneBucketBetweenNetworkAndBackpackInPriorityOrder();
        tests.outputCombinesMatchingFluidFromBothSourcesWithoutRequiringLocalTank();
        tests.backpackFirstAndNetworkUnavailableUseOnlyTheRequiredFallbackAmount();
        tests.simulationsDoNotConsumeSourcesDestinationsOrRemoteOriginState();
        tests.filteredOrRejectedFirstFluidDoesNotHideOtherNetworkFluids();
        tests.wholeBucketSelectionCanCombineLaterFluidEvenWhenEachSourceHasLessThanABucket();
        tests.wholeWorldBucketIsNotRemovedWhenCombinedDestinationHasInsufficientSpace();
        tests.destinationChangesAfterSimulationAndRemainderReturnsToSource();
        tests.nonReturnableRemainderStaysReservedAndStopsAfterOneInteraction();
        tests.componentsRemainDistinctAndArePreservedByRouting();
        tests.drainOnlyCapabilitiesWorkWithoutTankEnumeration();
        tests.emptyOrRejectedStorageNeverExtractsOrPersists();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void storageTransferGuards(GameTestHelper helper) {
        StorageTransferGuardTest tests = new StorageTransferGuardTest();
        tests.nativeIoBlocksSelfInsertionDuringDepositAndRecoversAfterwards();
        tests.guardPreservesNativeAllowAndBlockCompositionOutsideTransfers();
        tests.scopesAreNestedAndSharedAcrossWrappersWithTheSameContentsId();
        tests.anExceptionDoesNotLeaveExternalInsertionBlocked();
        tests.multipleLinkedUpgradesDoNotBypassAnExistingAllowList();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpWorldTargets(GameTestHelper helper) {
        PumpWorldTargetsTest tests = new PumpWorldTargetsTest();
        tests.worldPickupUsesNativeBoundedBreadthFirstOrder();
        tests.worldOutputAcceptsAirAndFlowingFluidButDoesNotReplaceSourcesOrSolidBlocks();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpUpgradeWrapper(GameTestHelper helper) throws ClassNotFoundException {
        RemotePumpUpgradeWrapperTest tests = new RemotePumpUpgradeWrapperTest();
        tests.everyNativeBackpackTierCanTransferAWholeBucketWithoutATank();
        tests.reservationSurvivesItemCopyWithNbtAndBlocksUnlinking();
        tests.nativePlacedBackpackClassLoadsWithTheFluidCapabilityMixin();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void prioritySettings(GameTestHelper helper) {
        PrioritySettingsTest tests = new PrioritySettingsTest();
        tests.priorityClickUpdatesDisplayedStateBeforePacketAndSurvivesReconstruction();
        tests.feedingFilterUsesCurrentWrapperAfterSynchronizationAndPreservesListMode();
        tests.pickupAndMagnetBackpackFirstUseNativeInventoryBeforeNetworkInBothPhases();
        tests.storedContentStillRequiresLocalItemOrMemoryWithBackpackFirst();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void adapterProfile(GameTestHelper helper) {
        StorageAdapterRegistryTest tests = new StorageAdapterRegistryTest();
        tests.optionalAdaptersStayUnavailableInTheBaseGameTestProfile();
        tests.connectionResultsCannotClaimConnectivityWithoutATarget();
        helper.succeed();
    }
}
