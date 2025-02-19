import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import edu.kiet.innogeeks.databinding.EachItemBinding
import edu.kiet.innogeeks.model.User


class UserListAdapter : ListAdapter<User, UserListAdapter.UserViewHolder>(UserDiffCallback()) {

    var onItemCheckedChanged: ((User, Boolean) -> Unit)? = null
    var onItemClicked: ((User) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = EachItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: EachItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            with(binding) {
                textName.text = user.name
                textEmail.text = user.email
                checkboxUser.isChecked = user.isSelected

                checkboxUser.setOnCheckedChangeListener { _, isChecked ->
                    onItemCheckedChanged?.invoke(user, isChecked)
                }

                root.setOnClickListener {
                    onItemClicked?.invoke(user)
                }
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}