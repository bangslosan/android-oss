package com.kickstarter.viewmodels

import android.util.Pair
import androidx.annotation.NonNull
import com.kickstarter.KSRobolectricTestCase
import com.kickstarter.libs.Environment
import com.kickstarter.mock.factories.ConfigFactory
import com.kickstarter.mock.factories.ProjectFactory
import com.kickstarter.mock.factories.RewardFactory
import com.kickstarter.models.Project
import com.kickstarter.models.Reward
import org.junit.Test
import rx.observers.TestSubscriber

class RewardFragmentViewModelTest: KSRobolectricTestCase() {

    private lateinit var vm: RewardFragmentViewModel.ViewModel
    private val conversionTextViewText = TestSubscriber.create<String>()
    private val conversionSectionIsGone = TestSubscriber.create<Boolean>()
    private val descriptionTextViewText = TestSubscriber<String>()
    private val isClickable = TestSubscriber<Boolean>()
    private val limitAndRemainingTextViewIsGone = TestSubscriber<Boolean>()
    private val limitAndRemainingTextViewText = TestSubscriber<Pair<String, String>>()
    private val minimumTextViewText = TestSubscriber<String>()
    private val rewardDescriptionIsGone = TestSubscriber<Boolean>()
    private val rewardsItemsAreGone = TestSubscriber<Boolean>()
    private val startBackingActivity = TestSubscriber<Project>()
    private val startCheckoutActivity = TestSubscriber<Pair<Project, Reward>>()
    private val titleTextViewIsGone = TestSubscriber<Boolean>()
    private val titleTextViewText = TestSubscriber<String>()

    private fun setUpEnvironment(@NonNull environment: Environment) {
        this.vm = RewardFragmentViewModel.ViewModel(environment)
        this.vm.outputs.conversionTextViewText().subscribe(this.conversionTextViewText)
        this.vm.outputs.conversionTextViewIsGone().subscribe(this.conversionSectionIsGone)
        this.vm.outputs.descriptionTextViewText().subscribe(this.descriptionTextViewText)
        this.vm.outputs.isClickable().subscribe(this.isClickable)
        this.vm.outputs.limitAndRemainingTextViewText().subscribe(this.limitAndRemainingTextViewText)
        this.vm.outputs.limitAndRemainingTextViewIsGone().subscribe(this.limitAndRemainingTextViewIsGone)
        this.vm.outputs.minimumTextViewText().subscribe(this.minimumTextViewText)
        this.vm.outputs.rewardDescriptionIsGone().subscribe(this.rewardDescriptionIsGone)
        this.vm.outputs.rewardsItemsAreGone().subscribe(this.rewardsItemsAreGone)
        this.vm.outputs.startBackingActivity().subscribe(this.startBackingActivity)
        this.vm.outputs.startCheckoutActivity().subscribe(this.startCheckoutActivity)
        this.vm.outputs.titleTextViewIsGone().subscribe(this.titleTextViewIsGone)
        this.vm.outputs.titleTextViewText().subscribe(this.titleTextViewText)
    }

    @Test
    fun testConversionHiddenForProject() {
        // Set the project currency and the user's chosen currency to the same value
        setUpEnvironment(environment())
        val project = ProjectFactory.project().toBuilder().currency("USD").currentCurrency("USD").build()
        val reward = RewardFactory.reward()

        // the conversion should be hidden.
        this.vm.inputs.projectAndReward(project, reward)
        this.conversionTextViewText.assertValueCount(1)
        this.conversionSectionIsGone.assertValue(true)
    }

    @Test
    fun testConversionShownForProject() {
        // Set the project currency and the user's chosen currency to different values
        setUpEnvironment(environment())
        val project = ProjectFactory.project().toBuilder().currency("CAD").currentCurrency("USD").build()
        val reward = RewardFactory.reward()

        // USD conversion should shown.
        this.vm.inputs.projectAndReward(project, reward)
        this.conversionTextViewText.assertValueCount(1)
        this.conversionSectionIsGone.assertValue(false)
    }

    @Test
    fun testConversionTextRoundsUp() {
        // Set user's country to US.
        val config = ConfigFactory.configForUSUser()
        val environment = environment()
        environment.currentConfig().config(config)
        setUpEnvironment(environment)

        // Set project's country to CA and reward minimum to $0.30.
        val project = ProjectFactory.caProject()
        val reward = RewardFactory.reward().toBuilder().minimum(0.3f).build()

        // USD conversion should be rounded up.
        this.vm.inputs.projectAndReward(project, reward)
        this.conversionTextViewText.assertValue("CA$ 1")
    }

    @Test
    fun testDescriptionTextViewText() {
        val project = ProjectFactory.project()
        val reward = RewardFactory.reward()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, reward)
        this.descriptionTextViewText.assertValue(reward.description())
    }

    @Test
    fun testGoToCheckoutWhenProjectIsSuccessful() {
        val project = ProjectFactory.successfulProject()
        val reward = RewardFactory.reward()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, reward)
        this.startCheckoutActivity.assertNoValues()

        this.vm.inputs.rewardClicked()
        this.startCheckoutActivity.assertNoValues()
    }

    @Test
    fun testGoToCheckoutWhenProjectIsSuccessfulAndHasBeenBacked() {
        val project = ProjectFactory.backedProject().toBuilder()
                .state(Project.STATE_SUCCESSFUL)
                .build()
        val reward = project.backing()?.reward()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, reward!!)
        this.startCheckoutActivity.assertNoValues()

        this.vm.inputs.rewardClicked()
        this.startCheckoutActivity.assertNoValues()
    }

    @Test
    fun testGoToCheckoutWhenProjectIsLive() {
        val reward = RewardFactory.reward()
        val liveProject = ProjectFactory.project()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(liveProject, reward)
        this.startCheckoutActivity.assertNoValues()

        // When a reward from a live project is clicked, start checkout.
        this.vm.inputs.rewardClicked()
        this.startCheckoutActivity.assertValue(Pair.create(liveProject, reward))
    }

    @Test
    fun testIsClickable() {
        setUpEnvironment(environment())

        // A reward from a live project should be clickable.
        this.vm.inputs.projectAndReward(ProjectFactory.project(), RewardFactory.reward())
        this.isClickable.assertValue(true)

        // A reward from a successful project should not be clickable.
        this.vm.inputs.projectAndReward(ProjectFactory.successfulProject(), RewardFactory.reward())
        this.isClickable.assertValues(true, false)
        //
        // A backed reward from a live project should be clickable.
        val backedLiveProject = ProjectFactory.backedProject()
        this.vm.inputs.projectAndReward(backedLiveProject, backedLiveProject.backing()?.reward()!!)
        this.isClickable.assertValues(true, false, true)

        // A backed reward from a finished project should be clickable (distinct until changed).
        val backedSuccessfulProject = ProjectFactory.backedProject().toBuilder()
                .state(Project.STATE_SUCCESSFUL)
                .build()
        this.vm.inputs.projectAndReward(backedSuccessfulProject, backedSuccessfulProject.backing()?.reward()!!)
        this.isClickable.assertValues(true, false, true)

        // A reward with its limit reached should not be clickable.
        this.vm.inputs.projectAndReward(ProjectFactory.project(), RewardFactory.limitReached())
        this.isClickable.assertValues(true, false, true, false)
    }

    @Test
    fun testLimitAndRemaining() {
        val project = ProjectFactory.project()
        setUpEnvironment(environment())

        // When reward is limited, quantity should be shown.
        val limitedReward = RewardFactory.reward().toBuilder()
                .limit(10)
                .remaining(5)
                .build()
        this.vm.inputs.projectAndReward(project, limitedReward)
        this.limitAndRemainingTextViewText.assertValue(Pair.create("10", "5"))
        this.limitAndRemainingTextViewIsGone.assertValue(false)

        // When reward's limit has been reached, don't show quantity.
        this.vm.inputs.projectAndReward(project, RewardFactory.limitReached())
        this.limitAndRemainingTextViewIsGone.assertValues(false, true)

        // When reward has no limit, don't show quantity (distinct until changed).
        this.vm.inputs.projectAndReward(project, RewardFactory.reward())
        this.limitAndRemainingTextViewIsGone.assertValues(false, true)
    }

    @Test
    fun testMinimumTextViewText() {
        val project = ProjectFactory.project()
        val reward = RewardFactory.reward().toBuilder()
                .minimum(10f)
                .build()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, reward)
        this.minimumTextViewText.assertValue("$10")
    }

    @Test
    fun testMinimumTextViewTextCAD() {
        val project = ProjectFactory.caProject()
        val reward = RewardFactory.reward().toBuilder()
                .minimum(10f)
                .build()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, reward)
        this.minimumTextViewText.assertValue("CA$ 10")
    }

    @Test
    fun testTitleTextViewText() {
        val project = ProjectFactory.project()
        setUpEnvironment(environment())

        // Reward with no title should be hidden.
        val rewardWithNoTitle = RewardFactory.reward().toBuilder()
                .title(null)
                .build()
        this.vm.inputs.projectAndReward(project, rewardWithNoTitle)
        this.titleTextViewIsGone.assertValues(true)
        this.titleTextViewText.assertNoValues()

        // Reward with title should be visible.
        val title = "Digital bundle"
        val rewardWithTitle = RewardFactory.reward().toBuilder()
                .title(title)
                .build()
        this.vm.inputs.projectAndReward(project, rewardWithTitle)
        this.titleTextViewIsGone.assertValues(true, false)
        this.titleTextViewText.assertValue(title)
    }

    @Test
    fun testNonEmptyRewardsDescriptionAreShown() {
        val project = ProjectFactory.project()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, RewardFactory.reward())
        this.rewardDescriptionIsGone.assertValue(false)
    }

    @Test
    fun testEmptyRewardsDescriptionAreGone() {
        val project = ProjectFactory.project()
        setUpEnvironment(environment())

        this.vm.inputs.projectAndReward(project, RewardFactory.noDescription())
        this.rewardDescriptionIsGone.assertValue(true)
    }
}