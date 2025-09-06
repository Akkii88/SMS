import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MessageSpacingDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.top = verticalSpaceHeight / 2
        outRect.bottom = verticalSpaceHeight / 2
    }
}
