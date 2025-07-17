package com.iobits.photo_to_video_slides_maker.utils

interface Constants {
    companion object{
        const val trim ="TRIM"
        const val slowMo ="SLOW-MO"
        const val music = "Music"
        const val reverse = "Reverse"
        const val slideShow = "SLIDE_SHOW"
        const val video = "VIDEO"
        const val textOnVideo = "TEXT"

        //in app item sku
        var ITEM_SKU_REMOVE_ADS_ONLY = "remove_ads" // this will only remove ads .

        var ITEM_SKU_GET_PREMIUM = "get_premium" //this will remove ads and also enable premium sounds

        //  var ITEM_SKU_PRO_USER_SUB = "pro_version"
        var ITEM_SKU_PRO_USER_SUB = "pro_version_trial"

        // .
        var subProductPremiumPrice = ""
        var PremiumPriceAfterDis = ""
    }
}