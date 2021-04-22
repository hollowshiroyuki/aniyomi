package eu.kanade.tachiyomi.ui.watcher.viewer.webtoon

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.viewer.WatcherTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

/**
 * Holder of the webtoon viewer that contains a episode transition.
 */
class WebtoonTransitionHolder(
    val layout: LinearLayout,
    viewer: WebtoonViewer
) : WebtoonBaseHolder(layout, viewer) {

    /**
     * Subscription for status changes of the transition page.
     */
    private var statusSubscription: Subscription? = null

    private val transitionView = WatcherTransitionView(context)

    /**
     * View container of the current status of the transition page. Child views will be added
     * dynamically.
     */
    private var pagesContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val paddingVertical = 48.dpToPx
        val paddingHorizontal = 32.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        val childMargins = 16.dpToPx
        val childParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(0, childMargins, 0, childMargins)
        }

        layout.addView(transitionView)
        layout.addView(pagesContainer, childParams)
    }

    /**
     * Binds the given [transition] with this view holder, subscribing to its state.
     */
    fun bind(transition: EpisodeTransition) {
        transitionView.bind(transition)

        transition.to?.let { observeStatus(it, transition) }
    }

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    override fun recycle() {
        unsubscribeStatus()
    }

    /**
     * Observes the status of the page list of the next/previous episode. Whenever there's a new
     * state, the pages container is cleaned up before setting the new state.
     */
    private fun observeStatus(episode: WatcherEpisode, transition: EpisodeTransition) {
        unsubscribeStatus()

        statusSubscription = episode.stateObserver
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                pagesContainer.removeAllViews()
                when (state) {
                    is WatcherEpisode.State.Wait -> {
                    }
                    is WatcherEpisode.State.Loading -> setLoading()
                    is WatcherEpisode.State.Error -> setError(state.error, transition)
                    is WatcherEpisode.State.Loaded -> setLoaded()
                }
                pagesContainer.isVisible = pagesContainer.isNotEmpty()
            }

        addSubscription(statusSubscription)
    }

    /**
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        removeSubscription(statusSubscription)
        statusSubscription = null
    }

    /**
     * Sets the loading state on the pages container.
     */
    private fun setLoading() {
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyle)

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            setText(R.string.transition_pages_loading)
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    /**
     * Sets the loaded state on the pages container.
     */
    private fun setLoaded() {
        // No additional view is added
    }

    /**
     * Sets the error state on the pages container.
     */
    private fun setError(error: Throwable, transition: EpisodeTransition) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(R.string.transition_pages_error, error.message)
        }

        val retryBtn = AppCompatButton(context).apply {
            wrapContent()
            setText(R.string.action_retry)
            setOnClickListener {
                val toEpisode = transition.to
                if (toEpisode != null) {
                    viewer.activity.requestPreloadEpisode(toEpisode)
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
    }
}