package com.example.inout2020_aimers.appblock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inout2020_aimers.R
import com.example.inout2020_aimers.appblock.database.BlockedApps
import com.example.inout2020_aimers.appblock.database.BlockedAppsRepository
import com.example.inout2020_aimers.appblock.database.BlockerDatabase
import com.example.inout2020_aimers.databinding.FragmentBlockerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BlockerFragment : Fragment(R.layout.fragment_blocker){

    private val TAG = "BlockerFragment"
    private lateinit var binding : FragmentBlockerBinding
    private lateinit var installedApps : ArrayList<AppListModel>

    private lateinit var bottomSheetAdapter: AppsAdapter

    private lateinit var viewModel: BlockerViewModel

    private lateinit var blockedAppList : ArrayList<BlockedApps>


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = BlockerDatabase(requireActivity().applicationContext)
        Log.d(TAG, "onCreate: Database created")
        val repository = BlockedAppsRepository(database)

        val factory = BlockerVMF(repository)

        viewModel = ViewModelProvider(requireActivity(),factory).get(BlockerViewModel::class.java)

        blockedAppList = ArrayList()

        viewModel.getBlockedApps().observe(viewLifecycleOwner, Observer {
            blockedAppList.clear()
            blockedAppList.addAll(it)
            bottomSheetAdapter.notifyDataSetChanged()
//            Log.d(TAG, "onViewCreated: BlockedAppList -> ${blockedAppList.toString()}")
            for (app in blockedAppList) {
                Log.d(TAG, "blockedAppList: App -> ${app.packageName}")
            }
            Log.d(TAG, "==========================================================")

            for (app in it) {
                Log.d(TAG, "it : App -> ${app.packageName}")
            }

        })


        binding = FragmentBlockerBinding.bind(view)

        // Initialization of BottomSheet

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bmSheet.layoutSelectAppsBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        installedApps = ArrayList()
        bottomSheetAdapter = AppsAdapter(installedApps,viewModel,blockedAppList)

        // BottomSheet RecyclerView Setup
        binding.bmSheet.rvSelectApps.apply {
            adapter = bottomSheetAdapter
            layoutManager =LinearLayoutManager(activity)
        }



        // Expanding select apps bottom sheet
        binding.toolBarBlockerFragment.setOnMenuItemClickListener { menuItem ->

            if (menuItem.itemId == R.id.selectApps){


                // Get App list
                if(isAccessGranted()){
                    val listApps = activity?.packageManager?.getInstalledPackages(0)

                    for( i in listApps!!){
                        val appIcon = i.applicationInfo.loadIcon(activity?.packageManager)
                        val appName = i.applicationInfo.loadLabel(activity?.packageManager!!).toString()
                        val appPackageName = i.applicationInfo.packageName.toString()

                        var isBlocked = false

                        if (blockedAppList.contains(appPackageName)){
                            isBlocked = true
                        }

                        installedApps.add(
                            AppListModel(
                                icon = appIcon,
                                name = appName,
                                packageName = appPackageName,
                                isBlocked = isBlocked
                            )
                        )
                    } // End For loop

                    bottomSheetAdapter.notifyDataSetChanged()

                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                }else{
                    Toast.makeText(requireContext(),"Give usage permission", Toast.LENGTH_SHORT).show()

                    // Redirecting to Usage Settings
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }


            }

            return@setOnMenuItemClickListener true
        }


    }

    // Usage stats permission
    fun isAccessGranted():Boolean{
        try {
            val applicationInfo = activity?.packageManager!!.getApplicationInfo(activity?.packageName!!,0)

            val appsOpsManager = activity?.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

            var mode = 0

            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT){
                mode = appsOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid,applicationInfo.packageName)

            }

            return (mode== AppOpsManager.MODE_ALLOWED)
        }catch (ex : PackageManager.NameNotFoundException){
            return false
        }
    }

}