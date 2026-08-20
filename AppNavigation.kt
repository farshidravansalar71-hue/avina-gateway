package com.avina.health.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.avina.health.screen.splash.SplashScreen
import com.avina.health.screen.onboarding.PersonalInfoScreen
import com.avina.health.screen.onboarding.WelcomeScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()


    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {


        composable("splash") {

            SplashScreen(
                onFinished = {

                    navController.navigate("welcome") {

                        popUpTo("splash") {
                            inclusive = true
                        }

                    }

                }
            )

        }



        composable("welcome") {

            WelcomeScreen(
                onStartClick = {

                    navController.navigate("personal_info")

                }
            )

        }



        composable("personal_info") {

            PersonalInfoScreen(
                onNextClick = { name, gender, age ->


                    navController.navigate("body_info")


                }
            )

        }


    }

}