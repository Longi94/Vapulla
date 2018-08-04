package `in`.dragonbra.vapulla.ui

import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.viewmodel.ViewModelFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import javax.inject.Inject

/**
 * Abstract fragment that all the other fragment should implement.
 */
abstract class VapullaFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity?.application as? VapullaApplication)?.graph?.inject(this)
    }
}
